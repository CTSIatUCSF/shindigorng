using System;
using System.Collections.Generic;
using System.Linq;
using System.Web;
using System.Web.Security;
using System.Web.UI;
using System.Web.UI.WebControls;
using System.Data;
using System.Data.Common;
using Connects.Profiles.Utility;
using Connects.Profiles.BusinessLogic;
using Microsoft.Practices.EnterpriseLibrary.Data;

public partial class gadgetlogin : BasePage
{
    protected void Page_Load(object sender, EventArgs e)
    {
        if (IsPostBack)
        {
            return;
        }
        // only allow access when in debug mode
        if (ConfigUtil.GetConfigItem("OpenSocialDevPassword") == null)
        {
            Response.Redirect("~/Search.aspx");
        }

        // Make sure the right panel is hidden
        HideRightColumn();

        Profile.PropertyValues.Clear();
        Profile.Save();

        OpenSocialHelper helper = new OpenSocialHelper(1, 1, Page);
        String gadgetURLs = "";
        foreach (PreparedGadget gadget in helper.GetVisibleGadgets())
        {
            gadgetURLs += gadget.GetGadgetURL() + Environment.NewLine;
        }
        txtGadgetURLS.Text = gadgetURLs;

        Session.Clear();
        Session.Abandon();
    }

    protected void btnGadgetLogin_Click(object sender, EventArgs e)
    {
        // first look at password
        if (!ConfigUtil.GetConfigItem("OpenSocialDevPassword").Equals(txtPassword.Text))
        {
            return;
        }
        
        String userName = null;

        try
        {
            Database db = DatabaseFactory.CreateDatabase();

            string sqlCommand = "select username from [user] where UserID = " + Int32.Parse(txtPersonId.Text) + ";";
            DbCommand dbCommand = db.GetSqlStringCommand(sqlCommand);
            userName = (String)db.ExecuteScalar(dbCommand);

            ProfilesMembershipUser user = (ProfilesMembershipUser)Membership.GetUser(userName);
            
            // Get an instance of the ProfileCommon object
            ProfileCommon p = (ProfileCommon)ProfileCommon.Create(user.UserName, true);

            // Set our parameters from the custom authentication provider
            p.UserId = user.UserID;
            p.UserName = user.UserName;
            p.HasProfile = user.HasProfile;
            p.ProfileId = user.ProfileID;
            p.DisplayName = user.DisplayName;
            
            // Persist the profile data
            p.Save();

            // Refetch the profile data
            Profile.Initialize(user.UserName, true);
            //Profile.GetProfile(user.UserName);

            FormsAuthentication.SetAuthCookie(userName, false);

            // add the gadgets
            Session[OpenSocialHelper.OPENSOCIAL_GADGETS] = txtGadgetURLS.Text;
            Session[OpenSocialHelper.OPENSOCIAL_DEBUG] = chkDebug.Checked;
            Session[OpenSocialHelper.OPENSOCIAL_NOCACHE] = !chkUseCache.Checked;
            Response.Redirect("~/Search.aspx");
        }
        catch (Exception ex)
        {
            // do nothing
        }
    }

}
