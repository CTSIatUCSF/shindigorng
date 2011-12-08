using System;
using System.Web;
using System.Web.UI;
using System.Collections.Generic;
using Connects.Profiles.Utility;
using Connects.Profiles.BusinessLogic;
using Connects.Profiles.Service.DataContracts;

/// <summary>
/// All Profiles web pages inherit from this
/// </summary>
public class BasePage : System.Web.UI.Page
{
    // Private fields used for BL access
    protected SystemBL _systemBL = new SystemBL();
    protected UserBL _userBL = new UserBL();
    protected PublicationBL _pubBL = new PublicationBL();
    protected string _NetworkBrowserService = ConfigUtil.GetConfigItem("NetworkBrowserService");
    private static string _redirectUrl;

    // Profiles OpenSocial Extension by UCSF
    private OpenSocialHelper osHelper = null;

    public BasePage()
    {
        _redirectUrl = string.Empty;
    }

    #region Page Properties

    public static string RedirectUrl {   
        get { return _redirectUrl; }
        set { _redirectUrl = value; }
    }

    #endregion

    #region Page Events

    protected override void OnPreInit(EventArgs e)
    {
        base.OnPreInit(e);
        if (Request.Browser.MSDomVersion.Major == 0) // If it is Non IE Browser
        {
            Response.Cache.SetNoStore();
        }
    }

    #endregion


    #region Helper Functions

    public Control FindControlRecursive(Control container, string name)
    {
        if (container.ID == name)
            return container;

        foreach (Control ctrl in container.Controls)
        {
            Control foundCtrl = FindControlRecursive(ctrl, name);
            if (foundCtrl != null)
                return foundCtrl;
        }
        return null;
    }

    protected void ShowControl(string name, bool visible)
    {
        //Find Control in Masterpage and Make it Invisible
        Control o = FindControlRecursive(Master, name);

        if (o != null)
            o.Visible = visible;
    }

    protected void RefreshUpdatePanel(string name)
    {
        UpdatePanel panel = (UpdatePanel)FindControlRecursive(Master, name);

        if (panel != null)
            panel.Update();
    }

    protected int GetPersonFromQueryString()
    {
        int personId = 0;

        if (Request.QueryString["Person"] != null)
        {
            if (!int.TryParse(Request.QueryString["Person"], out personId))
            {
                // get the internal personId from the profile search
                personId = new Connects.Profiles.Service.ServiceImplementation.ProfileService().GetPersonIdFromInternalId("EcommonsUsername", Request.QueryString["Person"]);

                string sourceQuery = Request.Url.Query;
                string personQuery = sourceQuery.Substring(sourceQuery.IndexOf("Person="), 11);

                // replace the original URL with person modified for the integer
                string newUrl = Request.Url.OriginalString.Replace(personQuery, "Person=" + personId.ToString());

                // redirect to the new URL
                Response.Redirect(newUrl);
            }
        }

        return personId;
    }

    protected void HideRightColumn()
    {
        // Swap the background image to make it look like a 2 column page.

        ((System.Web.UI.HtmlControls.HtmlGenericControl)Master.Master.FindControl("divCol3")).Visible = false;
        ((System.Web.UI.HtmlControls.HtmlGenericControl)Master.Master.FindControl("divCol13topSpace")).Visible = false;
        ((System.Web.UI.HtmlControls.HtmlGenericControl)Master.Master.FindControl("divCol1")).Style.Add("Width", String.Format("{0}px", ConfigUtil.GetConfigItem("TwoColWidth")));

        ((System.Web.UI.HtmlControls.HtmlGenericControl)Master.Master.FindControl("divContainer")).Style.Remove("background-image");

        ((System.Web.UI.HtmlControls.HtmlGenericControl)
            Master.Master.FindControl("divContainer")).Style.Add("background-image", String.Format("url(images/{0})", ConfigUtil.GetConfigItem("TwoColBackground")));

        ((System.Web.UI.HtmlControls.HtmlGenericControl)
            Master.Master.FindControl("divMainContainerBottom")).Style.Add("background-image", String.Format("url(images/{0})", "passive_bottom_alt.gif"));
    }

    protected void ShowRightColumn()
    {
        // Swap the background image to make it look like a 3 column page.

        ((System.Web.UI.HtmlControls.HtmlGenericControl)Master.Master.FindControl("divCol3")).Visible = true;
        ((System.Web.UI.HtmlControls.HtmlGenericControl)Master.Master.FindControl("divCol13topSpace")).Visible = true;
        ((System.Web.UI.HtmlControls.HtmlGenericControl)Master.Master.FindControl("divCol1")).Style.Add("Width", String.Format("{0}px", ConfigUtil.GetConfigItem("ThreeColWidth")));

        ((System.Web.UI.HtmlControls.HtmlGenericControl)Master.Master.FindControl("divContainer")).Style.Remove("background-image");

        ((System.Web.UI.HtmlControls.HtmlGenericControl)
            Master.Master.FindControl("divContainer")).Style.Add("background-image", String.Format("url(images/{0})", ConfigUtil.GetConfigItem("ThreeColBackground")));

        ((System.Web.UI.HtmlControls.HtmlGenericControl)
            Master.Master.FindControl("divMainContainerBottom")).Style.Add("background-image", String.Format("url(images/{0})", "passive_bottom.gif"));
    }

    protected void ShowMiniSearch(bool show)
    {
        ((System.Web.UI.Control)Master.Master.FindControl("left").FindControl("divLeftTop")).Visible = show;
    }

    protected void BindImage(byte[] image)
    {
        Response.BinaryWrite(image);
    }

    public string GetSortTextFromEnum(OutputOptionsSortType sortType)
    {
        string sortText = "";

        switch (sortType)
        {
            case OutputOptionsSortType.LastFirstName:
                sortText = "LastFirstName";
                break;
            case OutputOptionsSortType.Institution_Fullname:
                sortText = "Institution_Fullname";
                break;
            case OutputOptionsSortType.Department:
                sortText = "Department";
                break;
            case OutputOptionsSortType.Publications:
                sortText = "Publications";
                break;
            case OutputOptionsSortType.FacultyRank:
                sortText = "FacultyRank";
                break;
            case OutputOptionsSortType.PersonId:
                sortText = "PersonId";
                break;
            case OutputOptionsSortType.QueryRelevance:
                sortText = "QueryRelevance";
                break;
        }

        return sortText;
    }

    public OutputOptionsSortType GetSortEnumFromText(string sortSelection)
    {
        OutputOptionsSortType st = new OutputOptionsSortType();
        st = OutputOptionsSortType.LastFirstName;

        switch (sortSelection)
        {
            case "Institution_Fullname":
                st = OutputOptionsSortType.Institution_Fullname;
                break;
            case "Department":
                st = OutputOptionsSortType.Department;
                break;
            case "Publications":
                st = OutputOptionsSortType.Publications;
                break;
            case "LastFirstName":
                st = OutputOptionsSortType.LastFirstName;
                break;
            case "QueryRelevance":
                st = OutputOptionsSortType.QueryRelevance;
                break;
            case "FacultyRank":
                st = OutputOptionsSortType.FacultyRank;
                break;
            case "PersonId":
                st = OutputOptionsSortType.PersonId;
                break;
        }

        return st;
    }

    #endregion

    // Profiles OpenSocial Extension by UCSF
    #region OpenSocial
    protected OpenSocialHelper SetOpenSocialHelper(int viewerId, int ownerId, Page page)
    {
        // lazy create as this is expensive
        if (this.osHelper == null)
        {
            this.osHelper = new OpenSocialHelper(viewerId, ownerId, page);
        }
        return this.osHelper;
    }

    protected void GenerateOpensocialJavascipt()
    {
        System.Web.UI.Control pnlOpenSocialScripts = Master.Master.FindControl("HeadContentPlaceHolder").FindControl("pnlOpenSocialScripts");
        pnlOpenSocialScripts.Visible = osHelper.IsVisible();
        if (osHelper.IsVisible())
        {
            string gadgetScriptText = "<script type=\"text/javascript\" src=\"" + osHelper.GetContainerJavascriptSrc() + "\"></script>" + Environment.NewLine +
                "<script type=\"text/javascript\" language=\"javascript\">" + Environment.NewLine +
                "var my = {};" + Environment.NewLine +
                "my.gadgetSpec = function(appId, name, url, secureToken, view, closed_width, open_width, start_closed, chrome_id, visible_scope) {" + Environment.NewLine +
                    "this.appId = appId;" + Environment.NewLine +
                    "this.name = name;" + Environment.NewLine +
                    "this.url = url;" + Environment.NewLine +
                    "this.secureToken = secureToken;" + Environment.NewLine +
                    "this.view = view || 'default';" + Environment.NewLine +
                    "this.closed_width = closed_width;" + Environment.NewLine +
                    "this.open_width = open_width;" + Environment.NewLine +
                    "this.start_closed = start_closed;" + Environment.NewLine +
                    "this.chrome_id = chrome_id;" + Environment.NewLine +
                    "this.visible_scope = visible_scope;" + Environment.NewLine +
                    "};" + Environment.NewLine +
                "my.pubsubData = {};" + Environment.NewLine;
            foreach (KeyValuePair<string, string> pair in osHelper.GetPubsubData())
            {
                gadgetScriptText += "my.pubsubData['" + pair.Key + "'] = '" + pair.Value + "';" + Environment.NewLine;
            }
            gadgetScriptText += "my.openSocialURL = '" + ConfigUtil.GetConfigItem("OpenSocialURL") + "';" + Environment.NewLine +
                "my.debug = " + (osHelper.IsDebug() ? "1" : "0") + ";" + Environment.NewLine +
                "my.noCache = " + (osHelper.NoCache() ? "1" : "0") + ";" + Environment.NewLine +
                "my.gadgets = [";
            foreach (PreparedGadget gadget in osHelper.GetVisibleGadgets())
            {
                gadgetScriptText += "new my.gadgetSpec(" + gadget.GetAppId() + ",'" + gadget.GetName() + "','" + gadget.GetGadgetURL() + "','" +
                    gadget.GetSecurityToken() + "','" + gadget.GetView() + "'," + gadget.GetClosedWidth() + "," +
                    gadget.GetOpenWidth() + "," + (gadget.GetStartClosed() ? "1" : "0") + ",'" + gadget.GetChromeId() + "','" +
                    gadget.GetGadgetSpec().GetVisibleScope() + "'), ";
            }
            gadgetScriptText = gadgetScriptText.Substring(0, gadgetScriptText.Length - 2) + "];" + Environment.NewLine + "</script>" + Environment.NewLine +
            "<script type=\"text/javascript\" src=\"Scripts/profilesShindig.js\"></script>";

            ((System.Web.UI.WebControls.Literal)pnlOpenSocialScripts.FindControl("GadgetJavascriptLiteral")).Text = gadgetScriptText;

            // tools
            System.Web.UI.Control pnlOpenSocialTools = Master.Master.FindControl("left").FindControl("pnlOpenSocialTools");
            pnlOpenSocialTools.Visible = osHelper.IsVisible();
        }
    }

    protected OpenSocialHelper OpenSocial()
    {
        return osHelper;
    }

    [System.Web.Services.WebMethod]
    public static string onSubscribe(string sender, string channel, string pubsubHint)
    {
        return sender + " onSubscribe on channel " + channel + " with hint " + pubsubHint;
    }
    #endregion
}
