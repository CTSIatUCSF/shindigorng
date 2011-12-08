using System;
using System.Web.UI;
using System.Text;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Web.UI.WebControls;
using System.Web.SessionState;
using System.Web.Script.Serialization;
using Connects.Profiles.Common;
using Connects.Profiles.Service.DataContracts;
using Connects.Profiles.Utility;
using System.Web.UI.HtmlControls;
using System.Web;
using System.Data;
using System.Data.Common;
using System.Collections.Generic;
using Microsoft.Practices.EnterpriseLibrary.Data;

public class OpenSocialHelper
{
    public static string OPENSOCIAL_DEBUG = "OPENSOCIAL_DEBUG";
    public static string OPENSOCIAL_NOCACHE = "OPENSOCIAL_NOCACHE";
    public static string OPENSOCIAL_GADGETS = "OPENSOCIAL_GADGETS";

    public static string JSON_PERSONID_CHANNEL = "JSONPersonIds";
    public static string JSON_PMID_CHANNEL = "JSONPubMedIds";
    
    #region "LocalVars"

    internal Random myRandom = new Random();
    private List<PreparedGadget> gadgets = new List<PreparedGadget>();
    private Dictionary<string, string> pubsubdata = new Dictionary<string,string>();
    private int viewerId = -1;
    internal int ownerId = -1;
    internal bool isDebug = false;
    internal bool noCache = false;
    private string pageName;

    #endregion

    #region InitPage Helpers

    public OpenSocialHelper(int viewerId, int ownerId, Page page) 
    {
        this.viewerId = viewerId;
        this.ownerId = ownerId;
        this.isDebug = page.Session != null && page.Session[OPENSOCIAL_DEBUG] != null && (bool)page.Session[OPENSOCIAL_DEBUG];
        this.noCache = page.Session != null && page.Session[OPENSOCIAL_NOCACHE] != null && (bool)page.Session[OPENSOCIAL_NOCACHE];
        this.pageName = page.AppRelativeVirtualPath.Substring(2);

        if (ConfigUtil.GetConfigItem("OpenSocialURL") == null)
        {
            // do nothing
            return;
        }

        Random random = new Random();

        bool gadgetLogin = page.AppRelativeVirtualPath.EndsWith("gadgetlogin.aspx");
        String requestAppId = page.Request.QueryString["appId"];

        Dictionary<string, GadgetSpec> dbApps = new Dictionary<string, GadgetSpec>();
        Dictionary<string, GadgetSpec> officialApps = new Dictionary<string, GadgetSpec>();
        // Load gadgets from the DB first
        IDataReader reader = null;
        try
        {
            Database db = DatabaseFactory.CreateDatabase();

            string sqlCommand = "select appId, name, url, channels, enabled from shindig_apps";
            // if a specific app is requested, only grab it
            if (requestAppId != null)
            {
                sqlCommand += " where appId = " + requestAppId;
            }
            DbCommand dbCommand = db.GetSqlStringCommand(sqlCommand);
            reader = db.ExecuteReader(dbCommand);
            while (reader.Read())
            {
                GadgetSpec spec = new GadgetSpec(Convert.ToInt32(reader[0]), reader[1].ToString(), reader[2].ToString(), reader[3].ToString());
                string gadgetFileName = GetGadgetFileNameFromURL(reader[2].ToString());

                dbApps.Add(gadgetFileName, spec);
                if (requestAppId != null || Convert.ToBoolean(reader[4]))
                {
                    officialApps.Add(gadgetFileName, spec);
                }
            }
        }
        catch (Exception e)
        {
            throw new Exception(e.Message);
        }
        finally 
        {
            if (reader != null) 
            {
                reader.Close();
            }
        }


        // Add manual gadgets if there are any
        // Note that this block of code only gets executed after someone logs in with gadgetlogin.aspx!
        int moduleId = 0;
        if (page.Session != null && (string)page.Session[OPENSOCIAL_GADGETS] != null)
        {
            String openSocialGadgetURLS = (string)page.Session[OPENSOCIAL_GADGETS];
            String[] urls = openSocialGadgetURLS.Split(Environment.NewLine.ToCharArray());
            for (int i = 0; i < urls.Length; i++)
            {
                String openSocialGadgetURL = urls[i];
                if (openSocialGadgetURL.Length == 0)
                    continue;
                int appId = 0;  // if URL matches one in the DB, use DB provided appId, otherwise generate one
                string gadgetFileName = GetGadgetFileNameFromURL(openSocialGadgetURL);
                string name = gadgetFileName;
                string[] channels = new string[0];
                bool sandboxOnly = true;
                if (dbApps.ContainsKey(gadgetFileName))
                {
                    appId = dbApps[gadgetFileName].GetAppId();
                    name = dbApps[gadgetFileName].GetName();  
                    channels = dbApps[gadgetFileName].GetChannels();
                    sandboxOnly = false;
                }
                else
                {
                    CharEnumerator ce = openSocialGadgetURL.GetEnumerator();
                    while (ce.MoveNext())
                    {
                        appId += (int)ce.Current;
                    }
                }
                // if they asked for a specific one, only let it in
                if (requestAppId != null && Convert.ToInt32(requestAppId) != appId) 
                {
                    continue;
                }
                GadgetSpec gadget = new GadgetSpec(appId, name, openSocialGadgetURL, channels, sandboxOnly);
                // only add ones that are visible in this context!
                if (sandboxOnly || gadget.Show(viewerId, ownerId, page.AppRelativeVirtualPath.Substring(2)))
                {
                    String securityToken = SocketSendReceive(viewerId, ownerId, "" + gadget.GetAppId());
                    gadgets.Add(new PreparedGadget(gadget, this, moduleId++, securityToken));
                }
            }
        }

        // if no manual one were added, use the ones from the DB
        if (gadgets.Count == 0)
        {
            // Load DB gadgets
            if (gadgetLogin)
            {
                officialApps = dbApps;
            }
            foreach (KeyValuePair<string, GadgetSpec> pair in officialApps)
            {
                GadgetSpec gadget = new GadgetSpec(pair.Value.GetAppId(), pair.Value.GetName(), pair.Value.GetGadgetURL(), pair.Value.GetChannels(), false);
                // only add ones that are visible in this context!
                if (gadgetLogin || gadget.Show(viewerId, ownerId, GetPageName()))
                {
                    String securityToken = SocketSendReceive(viewerId, ownerId, "" + gadget.GetAppId());
                    gadgets.Add(new PreparedGadget(gadget, this, moduleId++, securityToken));
                }
            } 
        }
        // sort the gadgets
        gadgets.Sort();

        // trigger the javascript to render gadgets
        HtmlGenericControl body = (HtmlGenericControl)page.Master.FindControl("bodyMaster");
        if (body == null)
        {
            body = (HtmlGenericControl)page.Master.Master.FindControl("bodyMaster");
        }
        body.Attributes.Add("onload", "my.init();"); 
    }

    private string GetGadgetFileNameFromURL(string url)
    {
        string[] urlbits = url.ToString().Split('/');
        return urlbits[urlbits.Length - 1];
    }

    public bool IsDebug()
    {
        return isDebug;
    }

    public bool NoCache()
    {
        return noCache;
    }

    public bool HasGadgetListeningTo(string channel)
    {
        foreach (PreparedGadget gadget in GetVisibleGadgets())
        {
            if (gadget.GetGadgetSpec().ListensTo(channel))
            {
                return true;
            }
        }
        return false;
    }

    // JSON Helper Functions
    public static string BuildJSONPersonIds(List<Int32> personIds, string message)
    {
        Dictionary<string, Object> foundPeople = new Dictionary<string, object>();
        foundPeople.Add("personIds", personIds);
        foundPeople.Add("message", message);
        JavaScriptSerializer serializer = new JavaScriptSerializer();
        return serializer.Serialize(foundPeople);
    }

    public static string BuildJSONPersonIds(int personId, string message)
    {
        List<Int32> personIds = new List<Int32>();
        personIds.Add(personId);
        return BuildJSONPersonIds(personIds, message);
    }

    public static string BuildJSONPubMedIds(Person person)
    {
        List<Int32> pubIds = new List<Int32>();
        foreach (Publication pub in person.PublicationList)
        {
            foreach (PublicationSource pubSource in pub.PublicationSourceList)
            {
                if ("PubMed".Equals(pubSource.Name))
                {
                    pubIds.Add(Int32.Parse(pubSource.ID));
                }
            }
        }
        Dictionary<string, Object> foundPubs = new Dictionary<string, object>();
        foundPubs.Add("pubIds", pubIds);
        foundPubs.Add("message", "PubMedIDs for " + person.Name.FullName);
        JavaScriptSerializer serializer = new JavaScriptSerializer();
        return serializer.Serialize(foundPubs);
    }

    public void SetPubsubData(string key, string value)
    {
        if (pubsubdata.ContainsKey(key))
        {
            pubsubdata.Remove(key);
        }
        if (value != null || value.Length > 0)
        {
            pubsubdata.Add(key, value);
        }
    }

    public Dictionary<string, string> GetPubsubData()
    {
        return pubsubdata;
    }

    public void RemovePubsubGadgetsWithoutData()
    {
        // if any visible gadgets depend on pubsub data that isn't present, throw them out
        List<PreparedGadget> removedGadgets = new List<PreparedGadget>();
        foreach (PreparedGadget gadget in gadgets)
        {
            foreach (string channel in gadget.GetGadgetSpec().GetChannels())
            {
                if (!pubsubdata.ContainsKey(channel))
                {
                    removedGadgets.Add(gadget);
                    break;
                }
            }
        }
        foreach (PreparedGadget gadget in removedGadgets)
        {
            gadgets.Remove(gadget);
        }
    }

    public void RemoveGadget(string name)
    {
        // if any visible gadgets depend on pubsub data that isn't present, throw them out
        PreparedGadget gadgetToRemove = null;
        foreach (PreparedGadget gadget in gadgets)
        {
            if (name.Equals(gadget.GetName())) 
            {
                gadgetToRemove = gadget;
                break;
            }
        }
        gadgets.Remove(gadgetToRemove);
    }

    public string GetPageName()
    {
        return pageName;
    }

    public string GetContainerJavascriptSrc()
    {       
        return ConfigUtil.GetConfigItem("OpenSocialURL") + "/gadgets/js/core:dynamic-height:osapi:pubsub:rpc:views:shindig-container.js?c=1" +
            (isDebug ? "&debug=1" : "");
    }

    public string GetIdToUrlMapJavascript()
    {
        string retval = "var idToUrlMap = {";
        foreach (PreparedGadget gadget in gadgets)
        {
            //retval += gadget.GetAppId() + ":'" + gadget.GetGadgetURL() + "', ";
            retval += "'remote_iframe_" + gadget.GetAppId() + "':'" + gadget.GetGadgetURL() + "', ";
        }
        return retval.Substring(0, retval.Length - 2) + "};";
    }

    public bool IsVisible()
    {
        return (ConfigUtil.GetConfigItem("OpenSocialURL") != null && GetVisibleGadgets().Count > 0);
    }

    public List<PreparedGadget> GetVisibleGadgets()
    {
        return gadgets;
    }

    #endregion

    #region PostActivity
    public static void PostActivity(int userId, string title)
    {
        PostActivity(userId, title, null, null, null);
    }

    public static void PostActivity(int userId, string title, string body)
    {
        PostActivity(userId, title, body, null, null);
    }

    public static void PostActivity(int userId, string title, string body, string xtraId1Type, string xtraId1Value)
    {
        try
        {
            Database db = DatabaseFactory.CreateDatabase();

            string sqlCommand = "INSERT INTO shindig_activity (userId, activity, xtraId1Type, xtraId1Value) VALUES (" + userId +
                ",'<activity xmlns=\"http://ns.opensocial.org/2008/opensocial\"><postedTime>" + 
                Convert.ToInt64((DateTime.UtcNow - new DateTime(1970, 1, 1)).TotalMilliseconds) +"</postedTime><title>" + title + "</title>"
                + (body != null ? "<body>" + body + "</body>" : "") + "</activity>','" + xtraId1Type + "','" + xtraId1Value + "');";
            DbCommand dbCommand = db.GetSqlStringCommand(sqlCommand);
            db.ExecuteNonQuery(dbCommand);
        }
        catch (Exception e)
        {
            throw new Exception(e.Message);
        }
    }
    #endregion

    #region Socket Communications

    private static Socket ConnectSocket(string server, int port)
    {
        Socket s = null;
        IPHostEntry hostEntry = null;

        // Get host related information.
        hostEntry = Dns.GetHostEntry(server);

        // Loop through the AddressList to obtain the supported AddressFamily. This is to avoid
        // an exception that occurs when the host IP Address is not compatible with the address family
        // (typical in the IPv6 case).
        foreach (IPAddress address in hostEntry.AddressList)
        {
            IPEndPoint ipe = new IPEndPoint(address, port);
            Socket tempSocket =
                new Socket(ipe.AddressFamily, SocketType.Stream, ProtocolType.Tcp);

            tempSocket.Connect(ipe);

            if (tempSocket.Connected)
            {
                s = tempSocket;
                break;
            }
            else
            {
                continue;
            }
        }
        return s;
    }

    private static string SocketSendReceive(int viewer, int owner, string gadget)
    {
        //  These keys need to match what you see in edu.ucsf.profiles.shindig.service.SecureTokenGeneratorService in Shindig
        string[] tokenService = ConfigUtil.GetConfigItem("OpenSocialTokenService").Split(':');
        String request = "c=default&v=" + viewer + "&o=" + owner + "&g=" + gadget + "\r\n";
        Byte[] bytesSent = Encoding.ASCII.GetBytes(request);
        Byte[] bytesReceived = new Byte[256];

        // Create a socket connection with the specified server and port.
        Socket s = ConnectSocket(tokenService[0], Int32.Parse(tokenService[1]));

        if (s == null)
            return ("Connection failed");

        // Send request to the server.
        s.Send(bytesSent, bytesSent.Length, 0);

        // Receive the server home page content.
        int bytes = 0;
        string page = "";

        // The following will block until te page is transmitted.
        do
        {
            bytes = s.Receive(bytesReceived, bytesReceived.Length, 0);
            page = page + Encoding.ASCII.GetString(bytesReceived, 0, bytes);
        }
        while (bytes > 0);

        return page;
    }
    #endregion
}

public class GadgetViewRequirements
{
    private string page;
    private char viewerReq;  // U for User or null for no requirment
    private char ownerReq;   // R for Registered or null for no requirement
    private string view;
    private int closedWidth;
    private int openWidth;
    private bool startClosed;
    private string chromeId;
    private Int32 display_order;

    public GadgetViewRequirements(String page, char viewerReq, char ownerReq, String view, int closedWidth, int openWidth, bool startClosed, string chromeId, Int32 display_order)
    {
        this.page = page;
        this.viewerReq = viewerReq;
        this.ownerReq = ownerReq;
        this.view = view;
        this.closedWidth = closedWidth;
        this.openWidth = openWidth;
        this.startClosed = startClosed;
        this.chromeId = chromeId;
        this.display_order = display_order;
    }

    public char GetViewerReq()
    {
        return viewerReq;
    }

    public char GetOwnerReq()
    {
        return ownerReq;
    }

    public string GetView()
    {
        return view;
    }

    public int GetClosedWidth()
    {
        return closedWidth;
    }

    public int GetOpenWidth()
    {
        return openWidth;
    }

    public bool GetStartClosed()
    {
        return startClosed;
    }

    public string GetChromeId()
    {
        return chromeId;
    }

    internal Int32 GetDisplayOrder()
    {
        return display_order;
    }
}

public class GadgetSpec
{
    private string openSocialGadgetURL;
    private string name;
    private int appId = 0;
    private List<string> channels = new List<string>();
    private bool fromSandbox = false;
    private Dictionary<string, GadgetViewRequirements> viewRequirements = new Dictionary<string, GadgetViewRequirements>();

    // For preloading
    public GadgetSpec(int appId, string name, string openSocialGadgetURL, string[] channels)
    {
        this.appId = appId;
        this.name = name;
        this.openSocialGadgetURL = openSocialGadgetURL;
        this.channels.AddRange(channels);
    }

    public GadgetSpec(int appId, string name, string openSocialGadgetURL, string channelsStr)
        : this(appId, name, openSocialGadgetURL, channelsStr != null  && channelsStr.Length > 0 ? channelsStr.Split(' ') : new string[0])
    {
    }

    public GadgetSpec(int appId, string name, string openSocialGadgetURL, string[] channels, bool fromSandbox) 
        : this(appId, name, openSocialGadgetURL, channels)
    {
        this.fromSandbox = fromSandbox;
        // Load gadgets from the DB first
        if (!fromSandbox)
        {
            IDataReader reader = null;
            try
            {
                Database db = DatabaseFactory.CreateDatabase();

                string sqlCommand = "select page, viewer_req, owner_req, [view], closed_width, open_width, start_closed, chromeId, display_order from shindig_app_views where appId = " + appId;
                DbCommand dbCommand = db.GetSqlStringCommand(sqlCommand);
                reader = db.ExecuteReader(dbCommand);
                while (reader.Read())
                {
                    viewRequirements.Add(reader[0].ToString(), new GadgetViewRequirements(reader[0].ToString(),
                            reader.IsDBNull(1) ? ' ' : Convert.ToChar(reader[1]),
                            reader.IsDBNull(2) ? ' ' : Convert.ToChar(reader[2]),
                            reader[3].ToString(),
                            reader.IsDBNull(4) ? '0' : Convert.ToInt32(reader[4]),
                            reader.IsDBNull(5) ? '0' : Convert.ToInt32(reader[5]),
                            reader.IsDBNull(6) ? true : Convert.ToBoolean(reader[6]),
                            reader[7].ToString(),
                            reader.IsDBNull(8) ? Int32.MaxValue : Convert.ToInt32(reader[8])));
                }
            }
            catch (Exception e)
            {
                throw new Exception(e.Message);
            }
            finally
            {
                if (reader != null)
                {
                    reader.Close();
                }
            }
        }
    }

    public int GetAppId()
    {
        return appId;
    }

    public String GetName()
    {
        return name;
    }

    public String GetGadgetURL()
    {
        return openSocialGadgetURL;
    }

    public string[] GetChannels()
    {
        return channels.ToArray();
    }

    public bool ListensTo(string channel)
    {   // if fromSandbox just say yes, we don't care about performance in this situation
        return fromSandbox || channels.Contains(channel);
    }

    public GadgetViewRequirements GetGadgetViewRequirements(String page)
    {
        if (viewRequirements.ContainsKey(page))
        {
            return viewRequirements[page];
        }
        return null;
    }

    public bool Show(int viewerId, int ownerId, String page)
    {
        bool show = true;
        // if there are no view requirements, go ahead and show it.  We are likely testing out a new gadget
        // if there are some, turn it off unless this page is 
        if (viewRequirements.Count > 0)
        {
            show = false;
        }

        if (viewRequirements.ContainsKey(page))
        {
            show = true;
            GadgetViewRequirements req = GetGadgetViewRequirements(page);
            if ('U' == req.GetViewerReq() && viewerId <= 0)
            {
                show = false;
            }
            else if ('R' == req.GetViewerReq())
            {
                show &= IsRegisteredTo(viewerId);
            }
            if ('R' == req.GetOwnerReq())
            {
                show &= IsRegisteredTo(ownerId);
            }
        }
        return show;
    }

    public bool IsRegisteredTo(int personId)
    {
        Int32 count = 0;

        try
        {
            Database db = DatabaseFactory.CreateDatabase();

            string sqlCommand = "select count(*) from shindig_app_registry where appId = " + GetAppId() + " and personId = " + personId + ";";
            DbCommand dbCommand = db.GetSqlStringCommand(sqlCommand);
            count = (Int32)db.ExecuteScalar(dbCommand);
        }
        catch (Exception e)
        {
            throw new Exception(e.Message);
        }

        return (count == 1);
    }

    public bool FromSandbox()
    {
        return fromSandbox;
    }

    // who sees it?  Return the viewerReq for the ProfileDetails page
    public char GetVisibleScope()
    {
        GadgetViewRequirements req = GetGadgetViewRequirements("ProfileDetails.aspx");
        return req != null ? req.GetViewerReq() : ' ';
    }
}

public class PreparedGadget : IComparable<PreparedGadget>
{
    private GadgetSpec gadgetSpec;
    private OpenSocialHelper helper;
    private int moduleId;
    private string securityToken;

    public PreparedGadget(GadgetSpec gadgetSpec, OpenSocialHelper helper, int moduleId, string securityToken)
    {
        this.gadgetSpec = gadgetSpec;
        this.helper = helper;
        this.moduleId = moduleId;
        this.securityToken = securityToken;
    }

    public int CompareTo(PreparedGadget other)
    {
        GadgetViewRequirements gvr1 = this.GetGadgetViewRequirements();
        GadgetViewRequirements gvr2 = other.GetGadgetViewRequirements();
        return ("" + this.GetView() + (gvr1 != null ? gvr1.GetDisplayOrder() : Int32.MaxValue)).CompareTo("" + other.GetView() + (gvr2 != null ? gvr2.GetDisplayOrder() : Int32.MaxValue));
    }

    public GadgetSpec GetGadgetSpec()
    {
        return gadgetSpec;
    }

    public String GetSecurityToken()
    {
        return securityToken;
    }

    public int GetAppId()
    {
        return gadgetSpec.GetAppId();
    }

    public string GetName()
    {
        return gadgetSpec.GetName();
    }

    public int GetModuleId()
    {
        return moduleId;
    }

    public String GetGadgetURL()
    {
        return gadgetSpec.GetGadgetURL();
    }

    GadgetViewRequirements GetGadgetViewRequirements()
    {
        return gadgetSpec.GetGadgetViewRequirements(helper.GetPageName());
    }

    public String GetView()
    {
        GadgetViewRequirements reqs = GetGadgetViewRequirements();
        if (reqs != null)
        {
            return reqs.GetView();
        }
        // default behavior that will get invoked when there is no reqs.  Useful for sandbox gadgets
        else if (helper.GetPageName().Equals("ProfileEdit.aspx"))
        {
            return "home";
        }
        else if (helper.GetPageName().Equals("ProfileDetails.aspx"))
        {
            return "profile";
        }
        else if (helper.GetPageName().Equals("GadgetDetails.aspx"))
        {
            return "canvas";
        }
        else if (gadgetSpec.GetGadgetURL().Contains("Tool"))
        {
            return "small";
        }
        else
        {
            return null;
        }
    }

    public int GetOpenWidth()
    {
        GadgetViewRequirements reqs = GetGadgetViewRequirements();
        return reqs != null ? reqs.GetOpenWidth() : 0;
    }

    public int GetClosedWidth()
    {
        GadgetViewRequirements reqs = GetGadgetViewRequirements();
        return reqs != null ? reqs.GetClosedWidth() : 0;
    }
    
    public bool GetStartClosed()
    {
        GadgetViewRequirements reqs = GetGadgetViewRequirements();
        // if the page specific reqs are present, honor those.  Otherwise defaut to true for regular gadgets, false for sandbox gadgets
        return reqs != null ? reqs.GetStartClosed() : !gadgetSpec.FromSandbox();
    }

    public string GetChromeId()
    {
        GadgetViewRequirements reqs = GetGadgetViewRequirements();
        if (reqs != null)
        {
            return reqs.GetChromeId();
        }
        // default behavior that will get invoked when there is no reqs.  Useful for sandbox gadgets
        else if (gadgetSpec.GetGadgetURL().Contains("Tool"))
        {
            return "gadgets-tools";
        }
        else if (helper.GetPageName().Equals("ProfileEdit.aspx"))
        {
            return "gadgets-edit";
        }
        else if (helper.GetPageName().Equals("ProfileDetails.aspx"))
        {
            return "gadgets-view";
        }
        else if (helper.GetPageName().Equals("GadgetDetails.aspx"))
        {
            return "gadgets-detail";
        }
        else if (helper.GetPageName().Equals("Search.aspx"))
        {
            return "gadgets-search";
        }
        else
        {
            return null;
        }
    }

    public string Name
    {
        get { return gadgetSpec.GetName(); }
    }

    public string CanvasURL
    {
        get { return "~/GadgetDetails.aspx?appId=" + GetAppId() + "&Person=" + helper.ownerId; }
    }

    public int AppId
    {
        get { return GetAppId(); }
    }

    public int ModuleId
    {
        get { return GetModuleId(); }
    }

}

