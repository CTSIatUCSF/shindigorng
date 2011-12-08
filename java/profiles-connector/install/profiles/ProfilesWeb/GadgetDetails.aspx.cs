using System;
using System.Web.UI;
using System.Web.UI.WebControls;
using Connects.Profiles.Common;
using Connects.Profiles.Service.DataContracts;
using Connects.Profiles.Utility;

public partial class GadgetDetails : BasePage
{
    #region "LocalVars"


    #endregion

    #region Page Load Event
    protected void Page_Load(object sender, EventArgs e)
    {
        // for wiring up open social items
        SetOpenSocialHelper(Profile.UserId, -1, Page);
        GenerateOpensocialJavascipt();

        if (!IsPostBack)
        {
            hypBack.NavigateUrl = (string)Session["BackPage"];
        }
    }
    #endregion

}
