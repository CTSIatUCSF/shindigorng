/*  
 
    Copyright (c) 2008-2010 by the President and Fellows of Harvard College. All rights reserved.  
    Profiles Research Networking Software was developed under the supervision of Griffin M Weber, MD, PhD.,
    and Harvard Catalyst: The Harvard Clinical and Translational Science Center, with support from the 
    National Center for Research Resources and Harvard University.


    Code licensed under a BSD License. 
    For details, see: LICENSE.txt 
  
*/
using System;
using System.Collections;
using System.Data;
using System.Web;
using System.Web.UI;
using System.Web.UI.WebControls;
using System.Xml;
using Connects.Profiles.BusinessLogic;
using Connects.Profiles.Common;
using Connects.Profiles.Utility;
using Subgurim.Controles;



public partial class ProfileEdit : BasePageSecure
{
    #region Local Variables

    public DataSet PubMedResults
    {
        get
        {
            if (ViewState["PubMedResults"] == null)
            { ViewState["PubMedResults"] = new DataSet(); }
            return (DataSet)ViewState["PubMedResults"];
        }
        set
        {
            ViewState["PubMedResults"] = value;
        }
    }

    public int EditUserId
    {
        get
        {
            if (ViewState["EditUsername"] == null)
                ViewState["EditUsername"] = "";
            return Convert.ToInt32(ViewState["EditUsername"]);
        }
        set { ViewState["EditUsername"] = value; }
    }

    private Random myRandom = new Random();
    public int _personId = 0;
    private int _customPhotoSize = 256;
    private int _photoCount = 1;
    private int _customPhotoId = 9;

    #endregion

    #region Page Load Event
    protected void Page_Load(object sender, EventArgs e)
    {
        // a flag to inform the ucProfileBaseInfo that it is edit page
        Session["ProfileEdit"] = "true";

        _personId = GetPersonFromQueryString();

        if (_personId == 0)
        {
            if (Session["CurrentPersonEditing"] != null)
                _personId = System.Convert.ToInt32(Session["CurrentPersonEditing"]);
        }
        else
            Session["CurrentPersonEditing"] = _personId;

        if (FileUpload1.IsPosting)
            this.ProcessPhotoUpload();
        else
            FileUpload1.Reset();

        if (!IsPostBack)
        {
            Session["EmailImgText"] = "none";

            try
            {
                if (Request["Person"] != null)
                {
                    string backLink;

                    //Get User Preferences
                    GetUserPreferences(_personId);
     
                    backLink = "ProfileDetails.aspx?Person=" + GetPersonFromQueryString();

                    // Check to make sure this logged in users is an appropriate proxy
                    if (!_userBL.IsProxyFor(Profile.UserId, _personId) && (Profile.ProfileId != _personId))
                        Response.Redirect(backLink);

                    EditUserId = Profile.UserId;
                    Session["ProfileUsername"] = _personId;

                    ProfileRightSide1.ProfileId = _personId;

                    pnlControl.Visible = false;


                    //Populate User information
                    EditSection(_personId);

                    //imgReadPhoto.ImageUrl = _userBL.GetUserPhotoURL(personId);
                    ShowControl("pnlChkList", false);

                    //show back button
                    phEditModeLabel.Visible = true;
                    hypLnkReturn.NavigateUrl = backLink;

                    // Init photos for editing
                    EditPhotoSetup();

                    // Set help text
                    txtProfileProblem.Text = _userBL.GetProfileSupportHtml(_personId, false);

                    // Profiles OpenSocial Extension by UCSF
                    // for wiring up open social items
                    if (_userBL.IsProxyFor(Profile.UserId, _personId) || (Profile.ProfileId == _personId))
                    {
                        SetOpenSocialHelper(_personId, _personId, Page);
                        GenerateOpensocialJavascipt();
                        pnlOpenSocialGadgets.Visible = OpenSocial().IsVisible();
                    }

                    //Proxy settings 
                    if (_userBL.IsProxyFor(Profile.UserId, _personId) && ((string)Request["From"] == "Proxy"))
                    {
                        Control c = FindControlRecursive(Master, "hypEditAsProxy");
                        c.Visible = false;

                        //Find Control in Masterpage and Make it Visible
                        Control h = FindControlRecursive(Master, "hypViewThisProfile");
                        HyperLink hl = new HyperLink();
                        hl = (HyperLink)h;
                        hl.Visible = true;
                        hl.NavigateUrl = "ProfileDetails.aspx?Person=" + GetPersonFromQueryString();

                        IDataReader reader = null;

                        try
                        {
                            // PRG - Verify This
                            reader = _userBL.GetProxies(_personId, Profile.UserId, "Y", "Y", "Y", "Y");
                            if (reader.Read())
                            {
                                if (!Convert.ToBoolean(reader["editawards"]))
                                { pnlEditAwardsHonors.Visible = false; }
                                if (!Convert.ToBoolean(reader["editnarrative"]))
                                { pnlEditNarrative.Visible = false; }
                                if (!Convert.ToBoolean(reader["editpublications"]))
                                { pnlEditPublications.Visible = false; }
                            }
                        }
                        finally
                        {
                            if (reader != null)
                            { if (!reader.IsClosed) { reader.Close(); } }
                        }

                    }
                }
            }
            catch (Exception ex)
            {
                string err = ex.Message;
            }

            Page.Title = (string)Session["Fname"] + " " + (string)Session["Lname"] + " | " + Page.Title;

            // Make sure the right panel is hidden
            HideRightColumn();
        }
    }
    #endregion

    #region Get User Preferences
    private void GetUserPreferences(int personId)
    {
        try
        {
            UserPreferences userPreference = new UserPreferences();
            userPreference = _userBL.GetUserPreferences(personId);

            //If the user does not have a profile, then they need to be redirected back to the search screen.
            if (!userPreference.ProfileExists)
            {
                Response.Redirect("~/Search.aspx");                
            }


            //IF "Y" then Show 
            //IF "N" then Hide
            #region Hide/Show Photo
            if (userPreference.Photo.Equals("N"))
            {
                btnHidePhoto.Visible = false;
                lblHidePhoto.Visible = true;
                btnShowPhoto.Visible = true;
                lblShowPhoto.Visible = false;
                lblHiddenPhoto.Visible = true;
                lblVisiblePhoto.Visible = false;
            }
            else if (userPreference.Photo.Equals("Y"))
            {
                btnShowPhoto.Visible = false;
                lblShowPhoto.Visible = true;
                btnHidePhoto.Visible = true;
                lblHidePhoto.Visible = false;
                lblHiddenPhoto.Visible = false;
                lblVisiblePhoto.Visible = true;
            }
            #endregion

            #region Hide/Show Awards & Honors
            if (userPreference.AwardsHonors.Equals("N"))
            {
                btnHideAwards.Visible = false;
                lblHideAwards.Visible = true;
                btnShowAwards.Visible = true;
                lblShowAwards.Visible = false;
                lblHiddenAward.Visible = true;
                lblVisibleAward.Visible = false;
            }
            else if (userPreference.AwardsHonors.Equals("Y"))
            {
                btnShowAwards.Visible = false;
                lblShowAwards.Visible = true;
                btnHideAwards.Visible = true;
                lblHideAwards.Visible = false;
                lblHiddenAward.Visible = false;
                lblVisibleAward.Visible = true;
            }
            #endregion

            #region Hide/Show Narrative
            if (userPreference.Narrative.Equals("N"))
            {
                btnHideNarrative.Visible = false;
                lblHideNarrative.Visible = true;
                btnShowNarrative.Visible = true;
                lblShowNarrative.Visible = false;
                lblHiddenNarrative.Visible = true;
                lblVisibleNarrative.Visible = false;
            }
            else if (userPreference.Narrative.Equals("Y"))
            {
                btnShowNarrative.Visible = false;
                lblShowNarrative.Visible = true;
                btnHideNarrative.Visible = true;
                lblHideNarrative.Visible = false;
                lblHiddenNarrative.Visible = false;
                lblVisibleNarrative.Visible = true;
            }
            #endregion

            #region Hide/Show Publications
            if (userPreference.Publications.Equals("N"))
            {
                btnHidePublication.Visible = false;
                lblHidePublication.Visible = true;
                btnShowPublication.Visible = true;
                lblShowPublication.Visible = false;
                lblHiddenPublication.Visible = true;
                lblVisiblePublication.Visible = false;
            }
            else if (userPreference.Publications.Equals("Y"))
            {
                btnShowPublication.Visible = false;
                lblShowPublication.Visible = true;
                btnHidePublication.Visible = true;
                lblHidePublication.Visible = false;
                lblHiddenPublication.Visible = false;
                lblVisiblePublication.Visible = true;
            }
            #endregion

        }
        catch (Exception Ex)
        {
            throw (Ex);
        }


    }
    #endregion

    #region Load Profile Data
    private void EditSection(int personId)
    {
        try
        {
            #region Basic Information

            foreach (DataRow dro in _userBL.GetUserInformation(personId).Rows)
            {
                Session["PersonIsMy"] = (string)dro["Lastname"] + ", " + ((string)dro["Firstname"]).Substring(0, 1);

                ltProfileName.Text = (string)dro["DisplayName"];

                //Persist lastname into viewstate
                Session["Lname"] = (string)dro["Lastname"];
                Session["Fname"] = (string)dro["Firstname"];

                ucProfileBaseInfo.PersonId = GetPersonFromQueryString();
            }
            #endregion

            #region Narrative

            string narrative = _userBL.GetUserNarratives(personId);
            lblEditNarrative.Text = Server.HtmlEncode(narrative).Replace("\n", "<br />");
            txtEditNarrative.Text = narrative;
            if (narrative.Length == 0)
            {
                lblEditNarrative.Text = "<div style=\"font-style:italic; color:#999; \">None</div>";
            }

            #endregion

        }
        catch (Exception Ex)
        {
            string err = Ex.Message;
        }
    }
    #endregion

    #region Hide/Show User Address
    protected void btnShowAddress_OnClick(object sender, EventArgs e)
    {
        _userBL.SetUserPreferences(Convert.ToInt32(Session["ProfileUsername"]), "Address", "Y");
    }

    protected void btnHideAddress_OnClick(object sender, EventArgs e)
    {
        _userBL.SetUserPreferences(Convert.ToInt32(Session["ProfileUsername"]), "Address", "N");
    }

    #endregion

    #region Hide/Show User Email
    protected void btnShowEmail_OnClick(object sender, EventArgs e)
    {
        _userBL.SetUserPreferences(Convert.ToInt32(Session["ProfileUsername"]), "Email", "Y");
    }
   
    protected void btnHideEmail_OnClick(object sender, EventArgs e)
    {
        _userBL.SetUserPreferences(Convert.ToInt32(Session["ProfileUsername"]), "Email", "N");
    }

    #endregion
     
    #region Show User Photo
    protected void btnShowPhoto_OnClick(object sender, EventArgs e)
    {
        btnHidePhoto.Visible = true;
        lblHidePhoto.Visible = false;
        btnShowPhoto.Visible = false;
        lblShowPhoto.Visible = true;
        lblHiddenPhoto.Visible = false;
        lblVisiblePhoto.Visible = true;
        _userBL.SetUserPreferences(Convert.ToInt32(Session["ProfileUsername"]), "Photo", "Y");
        upnlEditSection.Update();
    }
    #endregion

    #region Hide User Photo
    protected void btnHidePhoto_OnClick(object sender, EventArgs e)
    {
        btnHidePhoto.Visible = false;
        lblHidePhoto.Visible = true;
        btnShowPhoto.Visible = true;
        lblShowPhoto.Visible = false;
        lblHiddenPhoto.Visible = true;
        lblVisiblePhoto.Visible = false;
        _userBL.SetUserPreferences(Convert.ToInt32(Session["ProfileUsername"]), "Photo", "N");
        upnlEditSection.Update();
    }
    #endregion

    #region Awards
    protected void btnShowAwards_OnClick(object sender, EventArgs e)
    {
        btnHideAwards.Visible = true;
        lblHideAwards.Visible = false;
        btnShowAwards.Visible = false;
        lblShowAwards.Visible = true;
        lblHiddenAward.Visible = false;
        lblVisibleAward.Visible = true;

        _userBL.SetUserPreferences(Convert.ToInt32(Session["ProfileUsername"]), "Awards", "Y");
        upnlEditSection.Update();
    }

    protected void btnHideAwards_OnClick(object sender, EventArgs e)
    {
        btnHideAwards.Visible = false;
        lblHideAwards.Visible = true;
        btnShowAwards.Visible = true;
        lblShowAwards.Visible = false;
        lblHiddenAward.Visible = true;
        lblVisibleAward.Visible = false;
        _userBL.SetUserPreferences(Convert.ToInt32(Session["ProfileUsername"]), "Awards", "N");
        upnlEditSection.Update();
    }

    protected void btnEditAwards_OnClick(object sender, EventArgs e)
    {
        if (pnlInsertAward.Visible)
        {
            btnInsertCancel_OnClick(sender, e);
        }
        else
        {
            btnImgEditAwards.ImageUrl = "~/Images/icon_squareDownArrow.gif";
            pnlInsertAward.Visible = true;
            SetFocus(txtFoot1);
        }
        upnlEditSection.Update();
    }

    protected void GridViewAwards_RowDataBound(object sender, GridViewRowEventArgs e)
    {
        if (e.Row.RowType == DataControlRowType.DataRow && e.Row.RowIndex == GridViewAwards.EditIndex)
        {
            TextBox text1 = new TextBox();
            TextBox text2 = new TextBox();
            TextBox text3 = new TextBox();
            TextBox text4 = new TextBox();

            text1 = (TextBox)e.Row.Cells[0].FindControl("txtYr1");
            text2 = (TextBox)e.Row.Cells[1].FindControl("txtYr2");
            text3 = (TextBox)e.Row.Cells[2].FindControl("txtAwardName");
            text4 = (TextBox)e.Row.Cells[3].FindControl("txtAwardInst");

            text1.Text = Server.HtmlDecode((string)text1.Text);
            text2.Text = Server.HtmlDecode((string)text2.Text);
            text3.Text = Server.HtmlDecode((string)text3.Text);
            text4.Text = Server.HtmlDecode((string)text4.Text);
        }
    }

    protected void GridViewAwards_RowEditing(object sender, GridViewEditEventArgs e)
    {
        upnlEditSection.Update();
    }

    protected void GridViewAwards_RowUpdating(object sender, GridViewUpdateEventArgs e)
    {
        // Iterate through the NewValues collection and HTML encode all 
        // user-provided values before updating the data source.
        foreach (DictionaryEntry entry in e.NewValues)
        {
            e.NewValues[entry.Key] = Server.HtmlEncode((string)entry.Value);
        }
    }

    protected void GridViewAwards_RowUpdated(object sender, GridViewUpdatedEventArgs e)
    {
        upnlEditSection.Update();
    }

    protected void GridViewAwards_RowCancelingEdit(object sender, GridViewCancelEditEventArgs e)
    {
        upnlEditSection.Update();
    }

    protected void GridViewAwards_RowDeleted(object sender, GridViewDeletedEventArgs e)
    {
        upnlEditSection.Update();
    }

    protected void btnInsertCancel_OnClick(object sender, EventArgs e)
    {
        txtFoot1.Text = "";
        txtFoot2.Text = "";
        txtFoot3.Text = "";
        txtFoot4.Text = "";
        pnlInsertAward.Visible = false;
        btnImgEditAwards.ImageUrl = "~/Images/icon_squareArrow.gif";
        upnlEditSection.Update();
    }

    protected void btnInsert_OnClick(object sender, EventArgs e)
    {
        if (txtFoot1.Text != "" || txtFoot2.Text != "" || txtFoot3.Text != "" || txtFoot4.Text != "")
        {
            AwardsDS.InsertParameters["yr"].DefaultValue = Server.HtmlEncode(txtFoot1.Text);
            AwardsDS.InsertParameters["yr2"].DefaultValue = Server.HtmlEncode(txtFoot2.Text);
            AwardsDS.InsertParameters["awardnm"].DefaultValue = Server.HtmlEncode(txtFoot3.Text);
            AwardsDS.InsertParameters["awardinginst"].DefaultValue = Server.HtmlEncode(txtFoot4.Text);
            AwardsDS.InsertParameters["PersonId"].DefaultValue = Session["ProfileUsername"].ToString();
            AwardsDS.Insert();
            GridViewAwards.DataBind();
            txtFoot1.Text = "";
            txtFoot2.Text = "";
            txtFoot3.Text = "";
            txtFoot4.Text = "";
            upnlEditSection.Update();
            // Profiles OpenSocial Extension by UCSF
            if (lblVisibleAward.Visible)
            {
                OpenSocialHelper.PostActivity(_personId, "added an award", "added an award: " + txtFoot3.Text);
            }
        }
    }

    protected void btnInsertClose_OnClick(object sender, EventArgs e)
    {
        if (txtFoot1.Text != "" || txtFoot2.Text != "" || txtFoot3.Text != "" || txtFoot4.Text != "")
        {
            AwardsDS.InsertParameters["yr"].DefaultValue = Server.HtmlEncode(txtFoot1.Text);
            AwardsDS.InsertParameters["yr2"].DefaultValue = Server.HtmlEncode(txtFoot2.Text);
            AwardsDS.InsertParameters["awardnm"].DefaultValue = Server.HtmlEncode(txtFoot3.Text);
            AwardsDS.InsertParameters["awardinginst"].DefaultValue = Server.HtmlEncode(txtFoot4.Text);
            AwardsDS.InsertParameters["PersonId"].DefaultValue = Session["ProfileUsername"].ToString();
            AwardsDS.Insert();
            GridViewAwards.DataBind();
            btnInsertCancel_OnClick(sender, e);
            // Profiles OpenSocial Extension by UCSF
            if (lblVisibleAward.Visible)
            {
                OpenSocialHelper.PostActivity(_personId, "added an award", "added an award: " + txtFoot3.Text);
            }
        }
    }
    #endregion

    #region Narrative
    protected void btnShowNarrative_OnClick(object sender, EventArgs e)
    {
        btnHideNarrative.Visible = true;
        lblHideNarrative.Visible = false;
        btnShowNarrative.Visible = false;
        lblShowNarrative.Visible = true;
        lblHiddenNarrative.Visible = false;
        lblVisibleNarrative.Visible = true;
        _userBL.SetUserPreferences(Convert.ToInt32(Session["ProfileUsername"]), "Narrative", "Y");
        upnlEditSection.Update();
    }

    protected void btnHideNarrative_OnClick(object sender, EventArgs e)
    {
        btnHideNarrative.Visible = false;
        lblHideNarrative.Visible = true;
        btnShowNarrative.Visible = true;
        lblShowNarrative.Visible = false;
        lblHiddenNarrative.Visible = true;
        lblVisibleNarrative.Visible = false;
        _userBL.SetUserPreferences(Convert.ToInt32(Session["ProfileUsername"]), "Narrative", "N");
        upnlEditSection.Update();
    }

    protected void btnEditNarrative_OnClick(object sender, EventArgs e)
    {
        if (pnlEditNarrativeText.Visible)
        {
            btnCancelNarrative_OnClick(sender, e);
        }
        else
        {
            btnImgEditNarrative.ImageUrl = "~/Images/icon_squareDownArrow.gif";
            lblEditNarrative.Visible = false;
            pnlEditNarrativeText.Visible = true;
        }
        upnlEditSection.Update();
    }

    protected void btnSaveNarrative_OnClick(object sender, EventArgs e)
    {
        _userBL.UpdateUserNarratives(_personId, txtEditNarrative.Text);

        lblEditNarrative.Visible = true;
        pnlEditNarrativeText.Visible = false;

        lblEditNarrative.Text = Server.HtmlEncode(txtEditNarrative.Text).Replace("\n", "<br />");

        if (txtEditNarrative.Text.Length == 0)
        {
            lblEditNarrative.Text = "<div style=\"font-style:italic; color:#999; \">None</div>";
        }
        // Profiles OpenSocial Extension by UCSF
        else
        {
            if (lblVisibleNarrative.Visible)
            {
                OpenSocialHelper.PostActivity(_personId, "edited their narrative");
            }
        }
        btnImgEditNarrative.ImageUrl = "~/Images/icon_squareArrow.gif";
        upnlEditSection.Update();
    }

    protected void btnCancelNarrative_OnClick(object sender, EventArgs e)
    {
        lblEditNarrative.Visible = true;
        txtEditNarrative.Text = _userBL.GetUserNarratives(Convert.ToInt32(Session["ProfileUsername"]));

        pnlEditNarrativeText.Visible = false;
        btnImgEditNarrative.ImageUrl = "~/Images/icon_squareArrow.gif";

        upnlEditSection.Update();
    }
    #endregion

    #region Publications
    protected void grdEditPublications_RowDataBound(object sender, GridViewRowEventArgs e)
    {
        if (e.Row.RowType == DataControlRowType.DataRow)
        {
            //HiddenField hdn = (HiddenField)e.Row.FindControl("hdnFromPubMed");
            //if (hdn.Value == "1")
            //{
            e.Row.Cells[1].Text = Server.HtmlDecode(e.Row.Cells[1].Text);
            //}

            if (!DataBinder.Eval(e.Row.DataItem, "mpid").Equals(System.DBNull.Value))
            {
                string str = (string)DataBinder.Eval(e.Row.DataItem, "mpid");

                if (str.Length > 0)
                {
                    LinkButton lb = (LinkButton)e.Row.FindControl("btnEditPublication");
                    lb.Visible = true;
                }
            }
        }
    }

    protected void grdEditPublications_SelectedIndexChanged(object sender, EventArgs e)
    {
        pnlAddPubById.Visible = false;
        pnlAddPubMed.Visible = false;
        pnlAddPubMedResults.Visible = false;
        pnlAddCustomPubMed.Visible = true;
        ClearPubMedCustom();

        IDataReader reader = null;
        try
        {
            HiddenField hdn = (HiddenField)grdEditPublications.Rows[grdEditPublications.SelectedIndex].FindControl("hdnMPID");
            reader = _pubBL.GetCustomPublication(hdn.Value);

            if (reader.Read())
            {
                //drpPublicationType.SelectedIndex = _customPhotoId;
                //if (drpPublicationType.Items.FindByValue(reader["hmspubcategory"].ToString()) != null)
                //{
                //    drpPublicationType.Items.FindByValue(reader["hmspubcategory"].ToString()).Selected = true;
                //    drpPublicationType.Enabled = false;
                //}
				
				drpPublicationType.SelectedValue = reader["hmspubcategory"].ToString();
				
                txtPubMedAdditionalInfo.Text = reader["additionalinfo"].ToString();
                txtPubMedAuthors.Text = reader["authors"].ToString();
                if (reader["hmspubcategory"].ToString() == "Thesis")
                { txtPubMedNewsCity.Text = reader["placeofpub"].ToString(); }
                else
                { txtPubMedPublisherCity.Text = reader["placeofpub"].ToString(); }
                txtPubMedNewsColumn.Text = reader["newspapercol"].ToString();
                txtPubMedConferenceDate.Text = reader["confdts"].ToString();
                txtPubMedConferenceEdition.Text = reader["confeditors"].ToString();
                txtPubMedConferenceName.Text = reader["confnm"].ToString();
                txtPubMedPublisherContract.Text = reader["contractnum"].ToString();

                if (reader["publicationdt"].ToString().Length > 0)
                {
                    DateTime dt = (DateTime.Parse(reader["publicationdt"].ToString()));
                    txtPubMedPublicationDate.Text = dt.ToShortDateString();
                }
                txtPubMedEdition.Text = reader["edition"].ToString();
                txtPubMedPublicationIssue.Text = reader["issuepub"].ToString();
                txtPubMedConferenceLocation.Text = reader["confloc"].ToString();
                txtPubMedPublisherName.Text = reader["publisher"].ToString();
                txtPubMedOptionalWebsite.Text = reader["url"].ToString();
                txtPubMedPublicationPages.Text = reader["paginationpub"].ToString();
                txtPubMedPublisherReport.Text = reader["reptnumber"].ToString();
                txtPubMedNewsSection.Text = reader["newspapersect"].ToString();
                txtPubMedTitle.Text = reader["pubtitle"].ToString();
                txtPubMedTitle2.Text = reader["articletitle"].ToString();
                txtPubMedNewsUniversity.Text = reader["dissunivnm"].ToString();
                txtPubMedPublicationVolume.Text = reader["volnum"].ToString();
                txtPubMedAbstract.Text = reader["abstract"].ToString();

                ShowCustomEdit(reader["hmspubcategory"].ToString());

                upnlEditSection.Update();

            }
        }
        catch (Exception ex)
        {
            string err = ex.Message;
        }
        finally
        {
            if (reader != null)
            {
                if (!reader.IsClosed)
                { reader.Close(); }
            }
        }
    }

    protected void grdEditPublications_RowDeleted(object sender, GridViewDeletedEventArgs e)
    {
        upnlEditSection.Update();
    }

    protected void btnShowPublication_OnClick(object sender, EventArgs e)
    {
        btnHidePublication.Visible = true;
        lblHidePublication.Visible = false;
        btnShowPublication.Visible = false;
        lblShowPublication.Visible = true;
        lblHiddenPublication.Visible = false;
        lblVisiblePublication.Visible = true;
        _userBL.SetUserPreferences(Convert.ToInt32(Session["ProfileUsername"]), "Publications", "Y");
        upnlEditSection.Update();
    }

    protected void btnHidePublication_OnClick(object sender, EventArgs e)
    {
        btnHidePublication.Visible = false;
        lblHidePublication.Visible = true;
        btnShowPublication.Visible = true;
        lblShowPublication.Visible = false;
        lblHiddenPublication.Visible = true;
        lblVisiblePublication.Visible = false;
        _userBL.SetUserPreferences(Convert.ToInt32(Session["ProfileUsername"]), "Publications", "N");
        upnlEditSection.Update();
    }
    #endregion

    #region Add PubMed By ID

    protected void btnAddPub_OnClick(object sender, EventArgs e)
    {
        if (pnlAddPubById.Visible)
        {
            btnDonePub_OnClick(sender, e);
        }
        else
        {
            btnImgAddPub.ImageUrl = "~/Images/icon_squareDownArrow.gif";

            phAddCustom.Visible = false;
            phAddPubMed.Visible = false;
            phDeletePub.Visible = false;
            pnlAddPubById.Visible = true;
            pnlAddPubMed.Visible = false;
            pnlAddPubMedResults.Visible = false;
            pnlAddCustomPubMed.Visible = false;
        }
        upnlEditSection.Update();
    }

    protected void btnDonePub_OnClick(object sender, EventArgs e)
    {
        phAddCustom.Visible = true;
        phAddPubMed.Visible = true;
        phDeletePub.Visible = true;
        txtPubId.Text = "";
        pnlAddPubById.Visible = false;
        btnImgAddPub.ImageUrl = "~/Images/icon_squareArrow.gif";
        upnlEditSection.Update();
    }

    protected void btnSavePub_OnClick(object sender, EventArgs e)
    {
        string inputString = txtPubId.Text.Trim();

        inputString = inputString.Replace(";", ",");
        inputString = inputString.Replace("\r\n", ",");
        inputString = inputString.Replace("\n", ",");
        inputString = inputString.Replace(" ", "");

        string[] PubIds = inputString.Split(',');
        string value = "";
        string seperator = "";
        foreach (string s in PubIds)
        {
            if (s.Length > 0)
            {
                value = value + seperator + s;
                seperator = ",";
            }
        }

        value = value.Trim();
        if (value.Length > 0)
        {
            string pubIdType = drpPubIdType.SelectedValue;
            try
            {
                switch (pubIdType.ToLower())
                {
                    case "pmid":
                        InsertPubMedIds(value);
                        break;
                }

                phAddCustom.Visible = true;
                phAddPubMed.Visible = true;
                phDeletePub.Visible = true;
                txtPubId.Text = "";
                pnlAddPubById.Visible = false;
                grdEditPublications.DataBind();
                btnImgAddPub.ImageUrl = "~/Images/icon_squareArrow.gif";
                upnlEditSection.Update();
            }
            catch (Exception ex)
            {
                string err = ex.Message;
            }
        }
    }

    //Inserts comma seperated string of PubMed Ids into the db
    private void InsertPubMedIds(string value)
    {
        CommonUtil commonUtil = new CommonUtil();
        string uri = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?retmax=1000&db=pubmed&retmode=xml&id=" + value;

        System.Xml.XmlDocument myXml = new System.Xml.XmlDocument();
        myXml.LoadXml(commonUtil.HttpPost(uri, "Catalyst", "text/plain"));
        XmlNodeList nodes = myXml.SelectNodes("PubmedArticleSet/PubmedArticle");
        foreach (XmlNode node in nodes)
        {
            string pmid = node.SelectSingleNode("MedlineCitation/PMID").InnerText;

            if (!_pubBL.CheckPublicationExists(pmid))
                // Insert or update the publication
                _pubBL.InsertPublication(pmid, node.OuterXml);

            // Assign the user to the publication
            _pubBL.AddUserPublication(_personId, Convert.ToInt32(pmid));
            // Profiles OpenSocial Extension by UCSF
            if (lblVisiblePublication.Visible)
            {
                OpenSocialHelper.PostActivity(_personId, "added a publication", "added PubMed publication PMID=" + pmid + " to their profile", "PMID", pmid);
            }
        }
    }

    #endregion

    #region Add PubMed By Search

    protected void btnAddPubMed_OnClick(object sender, EventArgs e)
    {

        if (pnlAddPubMed.Visible)
        {
            btnPubMedClose_OnClick(sender, e);
        }
        else
        {
            btnImgAddPubMed.ImageUrl = "~/Images/icon_squareDownArrow.gif";
            phAddCustom.Visible = false;
            phAddPub.Visible = false;
            phDeletePub.Visible = false;
            pnlAddPubMed.Visible = true;
            pnlAddPubById.Visible = false;
            pnlAddPubMedResults.Visible = false;
            pnlAddCustomPubMed.Visible = false;
        }

        upnlEditSection.Update();
    }

    private void ResetPubMedSearch()
    {
        txtSearchAffiliation.Text = "";
        txtSearchAuthor.Text = "";
        //txtSearchTitle.Text = "";
        txtSearchKeyword.Text = "";
        txtPubMedQuery.Text = "";
        rdoPubMedQuery.Checked = false;
        rdoPubMedKeyword.Checked = true;
        grdPubMedSearchResults.DataBind();
        lblPubMedResultsHeader.Text = "";
        pnlAddPubMedResults.Visible = false;
    }

    protected void btnPubMedClose_OnClick(object sender, EventArgs e)
    {
        ResetPubMedSearch();
        pnlAddPubMed.Visible = false;
        phAddCustom.Visible = true;
        phAddPub.Visible = true;
        phDeletePub.Visible = true;
        btnImgAddPubMed.ImageUrl = "~/Images/icon_squareArrow.gif";
        upnlEditSection.Update();
    }

    protected void btnPubMedReset_OnClick(object sender, EventArgs e)
    {
        ResetPubMedSearch();
        upnlEditSection.Update();
    }

    protected void btnPubMedSearch_OnClick(object sender, EventArgs e)
    {
        string value = "";

        if (rdoPubMedKeyword.Checked)
        {
            string andString = "";
            value = "(";
            if (txtSearchAuthor.Text.Length > 0)
            {
                string inputString = txtSearchAuthor.Text.Trim();

                inputString = inputString.Replace("\r\n", "|");
                // Added line to handle multiple authors for Firefox
                inputString = inputString.Replace("\n", "|");

                string[] split = inputString.Split('|');

                for (int i = 0; i < split.Length; i++)
                {
                    value = value + andString + "(" + split[i] + "[Author])";
                    andString = " AND ";
                }
            }
            if (txtSearchAffiliation.Text.Length > 0)
            {
                value = value + andString + "(" + txtSearchAffiliation.Text + "[Affiliation])";
                andString = " AND ";
            }
            if (txtSearchKeyword.Text.Length > 0)
            {
                value = value + andString + "((" + txtSearchKeyword.Text + "[Title/Abstract]) OR (" + txtSearchKeyword.Text + "[MeSH Terms]))";
            }
            value = value + ")";
        }
        else if (rdoPubMedQuery.Checked)
        {
            value = txtPubMedQuery.Text;
        }

        string orString = "";
        string idValues = "";
        //if (chkPubMedExclude.Checked)
        //{
        //    if (grdEditPublications.Rows.Count > 0)
        //    {
        //        value = value + " not (";
        //        foreach (GridViewRow gvr in grdEditPublications.Rows)
        //        {
        //            value = value + orString + (string)grdEditPublications.DataKeys[gvr.RowIndex]["PubID"]) + "[uid]";
        //            orString = " OR ";
        //        }
        //        value = value + ")";
        //    }
        //}

        if (chkPubMedExclude.Checked)
        {
            foreach (GridViewRow gvr in grdEditPublications.Rows)
            {
                HiddenField hdn = (HiddenField)gvr.FindControl("hdnPMID");
                idValues = idValues + orString + hdn.Value;
                orString = ",";
            }
        }

        CommonUtil commonUtil = new CommonUtil();
        Hashtable MyParameters = new Hashtable();

        string uri = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&usehistory=y&retmax=100&retmode=xml&term=" + value;
        System.Xml.XmlDocument myXml = new System.Xml.XmlDocument();
        myXml.LoadXml(commonUtil.HttpPost(uri, "Catalyst", "text/plain"));

        XmlNodeList xnList;
        string queryKey = "";
        string webEnv = "";

        xnList = myXml.SelectNodes("/eSearchResult");

        foreach (XmlNode xn in xnList)
        {
            queryKey = xn["QueryKey"].InnerText;
            webEnv = xn["WebEnv"].InnerText;
        }

        //string queryKey = MyGetXmlNodeValue(myXml, "QueryKey", "");
        //string webEnv = MyGetXmlNodeValue(myXml, "WebEnv", "");

        uri = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?retmin=0&retmax=100&retmode=xml&db=Pubmed&query_key=" + queryKey + "&webenv=" + webEnv;
        myXml.LoadXml(commonUtil.HttpPost(uri, "Catalyst", "text/plain"));

        string pubMedAuthors = "";
        string pubMedTitle = "";
        string pubMedSO = "";
        string pubMedID = "";
        string seperator = "";

        PubMedResults.Tables.Clear();
        PubMedResults.Tables.Add("Results");
        PubMedResults.Tables["Results"].Columns.Add(new System.Data.DataColumn("pmid"));
        PubMedResults.Tables["Results"].Columns.Add(new System.Data.DataColumn("citation"));
        PubMedResults.Tables["Results"].Columns.Add(new System.Data.DataColumn("checked"));

        XmlNodeList docSums = myXml.SelectNodes("eSummaryResult/DocSum");
        foreach (XmlNode docSum in docSums)
        {
            pubMedAuthors = "";
            pubMedTitle = "";
            pubMedSO = "";
            pubMedID = "";
            seperator = "";
            XmlNodeList authors = docSum.SelectNodes("Item[@Name='AuthorList']/Item[@Name='Author']");
            foreach (XmlNode author in authors)
            {
                pubMedAuthors = pubMedAuthors + seperator + author.InnerText;
                seperator = ", ";
            }
            pubMedTitle = docSum.SelectSingleNode("Item[@Name='Title']").InnerText;
            pubMedSO = docSum.SelectSingleNode("Item[@Name='SO']").InnerText;
            pubMedID = docSum.SelectSingleNode("Id").InnerText;

            if (!idValues.Contains(pubMedID))
            {
                DataRow myDataRow = PubMedResults.Tables["Results"].NewRow();
                myDataRow["pmid"] = pubMedID;
                myDataRow["checked"] = "0";
                myDataRow["citation"] = pubMedAuthors + "; " + pubMedTitle + "; " + pubMedSO;
                PubMedResults.Tables["Results"].Rows.Add(myDataRow);
                PubMedResults.AcceptChanges();
            }
        }

        grdPubMedSearchResults.DataSource = PubMedResults;
        grdPubMedSearchResults.DataBind();

        lblPubMedResultsHeader.Text = "PubMed Results (" + PubMedResults.Tables["Results"].Rows.Count.ToString() + ")";

        pnlAddPubMedResults.Visible = true;
        upnlEditSection.Update();
    }

    protected void grdPubMedSearchResults_RowDataBound(object sender, GridViewRowEventArgs e)
    {
        if (e.Row.RowType != DataControlRowType.DataRow) return;

        DataRowView drv = e.Row.DataItem as DataRowView;

        CheckBox cb = (CheckBox)e.Row.FindControl("chkPubMed");

        if (drv["checked"].ToString() == "0")
            cb.Checked = false;
        else
            cb.Checked = true;
    }

    protected void btnPubMedAddSelected_OnClick(object sender, EventArgs e)
    {
        string value = "";
        string seperator = "";
        //foreach (DataRow dr in PubMedResults.Tables["Results"].Rows)
        //{
        //    if ((string)dr["checked"]) == "1")
        //    {
        //        value = value + seperator + (string)dr["pmid"]);
        //    }
        //}
        foreach (GridViewRow row in grdPubMedSearchResults.Rows)
        {
            CheckBox cb = (CheckBox)row.FindControl("chkPubMed");
            if (cb.Checked)
            {
                value = value + seperator + (string)grdPubMedSearchResults.DataKeys[row.RowIndex]["pmid"];
                seperator = ",";
            }
        }

        InsertPubMedIds(value);

        //Clear form and grid after insert
        ResetPubMedSearch();
        grdPubMedSearchResults.DataBind();
        grdEditPublications.DataBind();
        pnlAddPubMedResults.Visible = false;
        pnlAddPubMed.Visible = false;
        phAddCustom.Visible = true;
        phAddPub.Visible = true;
        phDeletePub.Visible = true;
        btnImgAddPubMed.ImageUrl = "~/Images/icon_squareArrow.gif";
        upnlEditSection.Update();
    }

    protected void grdPubMedSearchResults_PageIndexChanging(object sender, GridViewPageEventArgs e)
    {
        //add checks to ds
        foreach (GridViewRow row in grdPubMedSearchResults.Rows)
        {
            CheckBox cb = (CheckBox)row.FindControl("chkPubMed");
            if (cb.Checked)
            {
                PubMedResults.Tables["Results"].Rows[row.RowIndex]["checked"] = "1";
            }
            else
            {
                PubMedResults.Tables["Results"].Rows[row.RowIndex]["checked"] = "0";
            }
        }
        PubMedResults.AcceptChanges();

        grdPubMedSearchResults.PageIndex = e.NewPageIndex;
        grdPubMedSearchResults.DataSource = PubMedResults;
        grdPubMedSearchResults.DataBind();
        upnlEditSection.Update();
    }

    protected void btnSelectAll_OnClick(object sender, EventArgs e)
    {
        foreach (GridViewRow row in grdPubMedSearchResults.Rows)
        {
            CheckBox cb = (CheckBox)row.FindControl("chkPubMed");
            cb.Checked = true;
        }
        upnlEditSection.Update();
    }

    protected void btnSelectNone_OnClick(object sender, EventArgs e)
    {
        foreach (GridViewRow row in grdPubMedSearchResults.Rows)
        {
            CheckBox cb = (CheckBox)row.FindControl("chkPubMed");
            cb.Checked = false;
        }
        upnlEditSection.Update();
    }

    #endregion

    #region Add PubMed Custom

    protected void btnAddCustom_OnClick(object sender, EventArgs e)
    {
        if (pnlAddCustomPubMed.Visible)
        {
            btnPubMedFinished_OnClick(sender, e);
        }
        else
        {
            btnImgAddCustom.ImageUrl = "~/Images/icon_squareDownArrow.gif";

            phAddPub.Visible = false;
            phAddPubMed.Visible = false;
            phDeletePub.Visible = false;
            pnlAddCustomPubMed.Visible = true;
            drpPublicationType.SelectedIndex = -1;
            drpPublicationType.Enabled = true;
            phMain.Visible = false;
            ClearPubMedCustom();
        }
        upnlEditSection.Update();
    }

    private void ClearPubMedCustom()
    {
        phAdditionalInfo.Visible = false;
        phAdditionalInfo2.Visible = false;
        phConferenceInfo.Visible = false;
        phEdition.Visible = false;
        phNewsUniversity.Visible = false;
        phPubIssue.Visible = false;
        phPublisherInfo.Visible = false;
        phPublisherName.Visible = false;
        phPublisherNumbers.Visible = false;
        phPubPageNumbers.Visible = false;
        phPubVolume.Visible = false;
        phTitle2.Visible = false;

        txtPubMedAdditionalInfo.Text = "";
        txtPubMedAuthors.Text = "";
        txtPubMedNewsCity.Text = "";
        txtPubMedPublisherCity.Text = "";
        txtPubMedNewsColumn.Text = "";
        txtPubMedConferenceDate.Text = "";
        txtPubMedConferenceEdition.Text = "";
        txtPubMedConferenceName.Text = "";
        txtPubMedPublisherContract.Text = "";
        txtPubMedPublicationDate.Text = "";
        txtPubMedEdition.Text = "";
        txtPubMedPublicationIssue.Text = "";
        txtPubMedConferenceLocation.Text = "";
        txtPubMedPublisherName.Text = "";
        txtPubMedOptionalWebsite.Text = "";
        txtPubMedPublicationPages.Text = "";
        txtPubMedPublisherReport.Text = "";
        txtPubMedNewsSection.Text = "";
        txtPubMedTitle.Text = "";
        txtPubMedTitle2.Text = "";
        txtPubMedNewsUniversity.Text = "";
        txtPubMedPublicationVolume.Text = "";
        txtPubMedAbstract.Text = "";
    }

    private void ShowCustomEdit(string publicationType)
    {
        phMain.Visible = true;


        switch (publicationType)
        {
            case "Abstracts":
                phTitle2.Visible = true;
                phPubIssue.Visible = true;
                phPubVolume.Visible = true;
                phPubPageNumbers.Visible = true;

                lblTitle.Text = "Title of Abstract";
                lblTitle2.Text = "Title of Publication";
                break;
            case "Books/Monographs/Textbooks":
                phTitle2.Visible = true;
                phEdition.Visible = true;
                phPubPageNumbers.Visible = true;
                phPublisherInfo.Visible = true;
                phPublisherName.Visible = true;
                phPublisherNumbers.Visible = true;
                phAdditionalInfo.Visible = true;
                phAdditionalInfo2.Visible = true;
                lblAdditionalInfo.Text = "For technical reports, sponsor info: Sponsered by the Agency for Health Care Policy and Research<br />For monograph in series, series editor info: Stoner GD, editor. Methods and perspectives in cell biology; vol 1.";

                lblPubMedPublisherReport.Text = "Report Number";
                lblPubMedPublisherContract.Text = "contract Number";

                lblTitle.Text = "Title of Book/Monograph";
                lblTitle2.Text = "Title of Book/Monograph Series with Editor Report";
                break;
            case "Clinical Communications":
                phTitle2.Visible = true;
                phEdition.Visible = true;
                phPubIssue.Visible = true;
                phPubVolume.Visible = true;
                phPubPageNumbers.Visible = true;
                phPublisherInfo.Visible = true;
                phPublisherName.Visible = true;
                phAdditionalInfo.Visible = true;
                phAdditionalInfo2.Visible = true;
                lblAdditionalInfo.Text = "Include description of who commissioned, purpose, users, penetration in summaryField.";

                lblTitle.Text = "Title of Communication";
                lblTitle2.Text = "Title of Journal/Book";
                break;
            case "Educational Materials":
                phAdditionalInfo.Visible = true;
                phAdditionalInfo2.Visible = true;
                lblAdditionalInfo.Text = "Brief description of educational context: e.g., presented at the Annual meeting of the Association of Academic Physiatrists, Las Vegas Indicate course and institution, if applicable";

                lblTitle.Text = "Title of Educational Materials";
                break;
            case "Non-Print Materials":
                phPublisherInfo.Visible = true;
                phPublisherName.Visible = true;
                phAdditionalInfo.Visible = true;
                phAdditionalInfo2.Visible = true;

                lblTitle.Text = "Title of Non-Print Materials";
                break;
            case "Original Articles":
                phTitle2.Visible = true;
                phPubIssue.Visible = true;
                phPubVolume.Visible = true;
                phPubPageNumbers.Visible = true;
                phNewsSection.Visible = true;

                lblTitle.Text = "Title of Article";
                lblTitle2.Text = "Title of Publication";
                break;
            case "Patents":
                phPublisherNumbers.Visible = true;
                lblPubMedPublisherReport.Text = "Sponsor/Assignee";
                lblPubMedPublisherContract.Text = "Patent Number";
                lblTitle.Text = "Title of Patent";
                break;
            case "Proceedings of Meetings":
                phTitle2.Visible = true;
                phPubIssue.Visible = true;
                phPubVolume.Visible = true;
                phPubPageNumbers.Visible = true;
                phPublisherInfo.Visible = true;
                phPublisherName.Visible = true;
                phConferenceInfo.Visible = true;

                lblTitle.Text = "Title of Paper";
                lblTitle2.Text = "Title of Publication";
                break;
            case "Reviews/Chapters/Editorials":
                phTitle2.Visible = true;
                phEdition.Visible = true;
                phPubIssue.Visible = true;
                phPubVolume.Visible = true;
                phPubPageNumbers.Visible = true;
                phPublisherInfo.Visible = true;
                phPublisherName.Visible = true;

                lblTitle.Text = "Title of Reviews/Chapters/Editorials";
                lblTitle2.Text = "Title of Publication (include editor if applicable)";
                break;
            case "Thesis":
                phNewsUniversity.Visible = true;

                lblTitle.Text = "Title of Thesis";
                break;
        }
    }

    protected void drpPublicationType_SelectedIndexChanged(object sender, EventArgs e)
    {
        ClearPubMedCustom();
        grdEditPublications.SelectedIndex = -1;

        if (drpPublicationType.SelectedIndex < 1)
        {
            phMain.Visible = false;
        }
        else
        {
            ShowCustomEdit(drpPublicationType.SelectedValue);
        }
        upnlEditSection.Update();
    }


    protected void btnPubMedSaveCustom_OnClick(object sender, EventArgs e)
    {
        Hashtable myParameters = new Hashtable();

        myParameters.Add("@HMS_PUB_CATEGORY", drpPublicationType.SelectedValue);
        myParameters.Add("@ADDITIONAL_INFO", txtPubMedAdditionalInfo.Text);
        myParameters.Add("@ABSTRACT", txtPubMedAbstract.Text);
        myParameters.Add("@AUTHORS", txtPubMedAuthors.Text);
        if (drpPublicationType.SelectedValue == "Thesis")
        { myParameters.Add("@PLACE_OF_PUB", txtPubMedNewsCity.Text); }
        else
        { myParameters.Add("@PLACE_OF_PUB", txtPubMedPublisherCity.Text); }
        myParameters.Add("@NEWSPAPER_COL", txtPubMedNewsColumn.Text);
        myParameters.Add("@CONF_DTS", txtPubMedConferenceDate.Text);
        myParameters.Add("@CONF_EDITORS", txtPubMedConferenceEdition.Text);
        myParameters.Add("@CONF_NM", txtPubMedConferenceName.Text);
        myParameters.Add("@CONTRACT_NUM", txtPubMedPublisherContract.Text);
        myParameters.Add("@PUBLICATION_DT", txtPubMedPublicationDate.Text);
        myParameters.Add("@EDITION", txtPubMedEdition.Text);
        myParameters.Add("@ISSUE_PUB", txtPubMedPublicationIssue.Text);
        myParameters.Add("@CONF_LOC", txtPubMedConferenceLocation.Text);
        myParameters.Add("@PUBLISHER", txtPubMedPublisherName.Text);
        myParameters.Add("@URL", txtPubMedOptionalWebsite.Text);
        myParameters.Add("@PAGINATION_PUB", txtPubMedPublicationPages.Text);
        myParameters.Add("@REPT_NUMBER", txtPubMedPublisherReport.Text);
        myParameters.Add("@NEWSPAPER_SECT", txtPubMedNewsSection.Text);
        myParameters.Add("@PUB_TITLE", txtPubMedTitle.Text);
        myParameters.Add("@ARTICLE_TITLE", txtPubMedTitle2.Text);
        myParameters.Add("@DISS_UNIV_NM", txtPubMedNewsUniversity.Text);
        myParameters.Add("@VOL_NUM", txtPubMedPublicationVolume.Text);

        if (grdEditPublications.SelectedIndex > -1)
        {
            //myParameters.Add("@username", Profile.UserId);
            myParameters.Add("@updated_by", EditUserId);
            HiddenField hdn = (HiddenField)grdEditPublications.Rows[grdEditPublications.SelectedIndex].FindControl("hdnMPID");
            myParameters.Add("@mpid", hdn.Value);

            _pubBL.EditCustomPublication(myParameters);
            grdEditPublications.SelectedIndex = -1;
            // Profiles OpenSocial Extension by UCSF
            if (lblVisiblePublication.Visible)
            {
                OpenSocialHelper.PostActivity(_personId, "updated a custom publication", "updated a custom publication: " + drpPublicationType.SelectedValue);
            }
        }
        else
        {
            myParameters.Add("@PersonID", _personId);
            myParameters.Add("@created_by", EditUserId);
            _pubBL.AddCustomPublication(myParameters);
            // Profiles OpenSocial Extension by UCSF
            if (lblVisiblePublication.Visible)
            {
                OpenSocialHelper.PostActivity(_personId, "added a custom publication", "added a custom publication: " + drpPublicationType.SelectedValue);
            }
        }

        grdEditPublications.DataBind();
        ClearPubMedCustom();

        LinkButton lb = (LinkButton)sender;
        if (lb.ID == "btnPubMedSaveCustom")
        {
            phAddPub.Visible = true;
            phAddPubMed.Visible = true;
            phDeletePub.Visible = true;
            phMain.Visible = false;
            pnlAddCustomPubMed.Visible = false;
            btnImgAddCustom.ImageUrl = "~/Images/icon_squareArrow.gif";
        }

        upnlEditSection.Update();
    }

    protected void btnPubMedFinished_OnClick(object sender, EventArgs e)
    {
        phAddPub.Visible = true;
        phAddPubMed.Visible = true;
        phDeletePub.Visible = true;
        ClearPubMedCustom();
        drpPublicationType.SelectedIndex = -1;
        grdEditPublications.SelectedIndex = -1;
        phMain.Visible = false;
        pnlAddCustomPubMed.Visible = false;
        btnImgAddCustom.ImageUrl = "~/Images/icon_squareArrow.gif";

        upnlEditSection.Update();
    }

    protected void btnPubMedById_Click(object sender, EventArgs e)
    {
        btnPubMedFinished_OnClick(sender, e);
        btnAddPubMed_OnClick(sender, e);

        upnlEditSection.Update();
    }

    #endregion

    #region DeletePubMed

    protected void btnDeletePub_OnClick(object sender, EventArgs e)
    {
        if (pnlDeletePubMed.Visible)
        {
            btnDeletePubMedClose_OnClick(sender, e);
        }
        else
        {
            btnImgDeletePub.ImageUrl = "~/Images/icon_squareDownArrow.gif";
            phAddCustom.Visible = false;
            phAddPubMed.Visible = false;
            phAddPub.Visible = false;
            pnlDeletePubMed.Visible = true;
            pnlAddPubById.Visible = false;
            pnlAddPubMed.Visible = false;
            pnlAddPubMedResults.Visible = false;
            pnlAddCustomPubMed.Visible = false;
        }
        upnlEditSection.Update();
    }

    protected void btnDeletePubMedOnly_OnClick(object sender, EventArgs e)
    {
        // PRG: Double-check we're using the correct username variable here
        //myParameters.Add("@username", (string)Session["ProfileUsername"]));
        _pubBL.DeletePublications(_personId, true, false);

        phAddPub.Visible = true;
        phAddPubMed.Visible = true;
        phAddCustom.Visible = true;
        pnlDeletePubMed.Visible = false;
        btnImgDeletePub.ImageUrl = "~/Images/icon_squareArrow.gif";

        grdEditPublications.DataBind();
        upnlEditSection.Update();
    }

    protected void btnDeleteCustomOnly_OnClick(object sender, EventArgs e)
    {
        // PRG: Double-check we're using the correct username variable here
        //myParameters.Add("@username", (string)Session["ProfileUsername"]));
        _pubBL.DeletePublications(_personId, false, true);

        phAddPub.Visible = true;
        phAddPubMed.Visible = true;
        phAddCustom.Visible = true;
        pnlDeletePubMed.Visible = false;
        btnImgDeletePub.ImageUrl = "~/Images/icon_squareArrow.gif";

        grdEditPublications.DataBind();
        upnlEditSection.Update();
    }

    protected void btnDeleteAll_OnClick(object sender, EventArgs e)
    {
        // PRG: Double-check we're using the correct username variable here
        //myParameters.Add("@username", (string)Session["ProfileUsername"]));
        _pubBL.DeletePublications(_personId, true, true);

        phAddPub.Visible = true;
        phAddPubMed.Visible = true;
        phAddCustom.Visible = true;
        pnlDeletePubMed.Visible = false;
        btnImgDeletePub.ImageUrl = "~/Images/icon_squareArrow.gif";

        grdEditPublications.DataBind();
        upnlEditSection.Update();
    }

    protected void btnDeletePubMedClose_OnClick(object sender, EventArgs e)
    {
        phAddPubMed.Visible = true;
        phAddCustom.Visible = true;
        phAddPub.Visible = true;
        phDeletePub.Visible = true;
        pnlDeletePubMed.Visible = false;
        btnImgDeletePub.ImageUrl = "~/Images/icon_squareArrow.gif";
        upnlEditSection.Update();
    }

    #endregion

    #region Photo Edit

    /// <summary>
    /// Sets the initial photo image
    /// </summary>
    protected void EditPhotoSetup()
    {
        this.imgEditPhoto.ImageUrl = _userBL.GetUserPhotoURL(_personId);
    }

    /// <summary>
    /// Handles the processing of uploaded files
    /// </summary>
    protected void ProcessPhotoUpload()
    {
        HttpPostedFileAJAX pf = this.FileUpload1.PostedFile;

        // check for size
        if (pf.ContentLength > (_customPhotoSize * 1024))// || (pf.Type != HttpPostedFileAJAX.fileType.image)
        {
            lblFileUploadError.Text = "File is too large";
            return;
        }

        if (!((pf.ContentType.Contains("bmp")) ||
                    (pf.ContentType.Contains("gif")) ||
                    (pf.ContentType.Contains("jpg")) ||
                    (pf.ContentType.Contains("jpeg")) ||
                    (pf.ContentType.Contains("png"))))
        {
            lblFileUploadError.Text = "File is not the correct type";
            return;
        }

        // create a byte array to store the file bytes
        byte[] fileBytes = new byte[pf.ContentLength];

        // fill the byte array
        using (System.IO.Stream stream = FileUpload1.FileContent)
        {
            stream.Read(fileBytes, 0, pf.ContentLength);
        }

        // Save the uploaded image to the DB
        _userBL.SaveUserPhoto(_personId, fileBytes);
        // Also set the user's preference
        _userBL.SetUserPreferences(_personId, "PhotoPreference", _customPhotoId.ToString());

        pf.Saved = true;

        // Buil URL to fetch image from database.  Note:  you must append the random number to the URL or the browser will cache the 
        // image and never let it refresh.
        string js = "var img;";
        js += "var lnk;";    
        js += "var pu;";
        
        js += "CheckVars();";

        js += "function CheckVars(){";
        js += "  img  = window.parent.document.getElementById('ctl00_ctl00_middle_MiddleContentPlaceHolder_imgEditPhoto3');";


        js += " if (img == null) {";
        js += "img = document.getElementById('middle_MiddleContentPlaceHolder_imgEditPhoto3');";
        js += "}";
        js += "if (img == null) { return; }";        
        js += " img.src = '";        
        js += string.Format("Thumbnail.ashx?id={0}&random={1}", _personId, new Random().Next().ToString());
        js += "';";

        js += "  lnk  = window.parent.document.getElementById('divPhotoUpload');";
        js += "  pu  = window.parent.document.getElementById('ctl00_ctl00_middle_MiddleContentPlaceHolder_lnkAddCustomPhoto');";
        js += " if (pu == null) {";
        js += "pu = document.getElementById('middle_MiddleContentPlaceHolder_lnkAddCustomPhoto');";
        js += "}";
        js += "if (pu == null) { return; }";        
        js += " pu.style.display='none';";
        js += " if (img == null) {";
        js += "lnk = document.getElementById('middle_MiddleContentPlaceHolder_lnkAddCustomPhoto');";
        js += "}";
        js += "if (lnk == null) { return; }";        
        js += "lnk.style.display='none';";
        js += "}";






        //// Buil URL to fetch image from database.  Note:  you must append the random number to the URL or the browser will cache the 
        //// image and never let it refresh.
        //string js = "var img = window.parent.document.getElementById('ctl00_ctl00_middle_MiddleContentPlaceHolder_imgEditPhoto3');";


        //js += " if (img == null) {";
        //js += "img = document.getElementById('middle_MiddleContentPlaceHolder_imgEditPhoto3');";
        //js += "}";
        //js += "if (img == null) { return; }";
        //js += " img.src = '";
        //js += string.Format("Thumbnail.ashx?id={0}&random={1}", _personId, new Random().Next().ToString());
        //js += "';";

        //js += "var pu = window.parent.document.getElementById('divPhotoUpload'); pu.style.display='none';";
        //js += "var lnk = window.parent.document.getElementById('ctl00_ctl00_middle_MiddleContentPlaceHolder_lnkAddCustomPhoto');";

        //js += " if (img == null) {";
        //js += "lnk = document.getElementById('middle_MiddleContentPlaceHolder_lnkAddCustomPhoto');";
        //js += "}";
        //js += "if (lnk == null) { return; }";
        //js += "lnk.style.display='none';";


        FileUpload1.addCustomJS(FileUploaderAJAX.customJSevent.postUpload, js);

        FileUpload1.Reset();

        // Profiles OpenSocial Extension by UCSF
        if (lblVisiblePhoto.Visible)
        {
            OpenSocialHelper.PostActivity(_personId, "uploaded a photo");
        }
    }

    /// <summary>
    /// Properly sets the radio buttons with the appropriate selection
    /// </summary>
    protected void SetPhotoSelection()
    {
        UserPreferences userPreference = new UserPreferences();
        userPreference = _userBL.GetUserPreferences(_personId);

       
        // For the custom photos, iterate through them and set checked to false
        for (int i = 0; i < dlPhotos.Items.Count; i++)
        {
            ((RadioButton)dlPhotos.Items[i].FindControl("rbPhoto")).Checked = false;
        }

        // For the single custom photo, just set it manually
        rbPhotoCustom.Checked = false;

        // Now set the initial state.  Check for custom first.
        if (userPreference.PhotoPref == _customPhotoId)
        {
            rbPhotoCustom.Checked = true;

            // Also set the hidden tracking value used for Javascript actions
            hidRbTrack.Value = _customPhotoId.ToString();
        }
        else
        {
            if (dlPhotos.Items.Count > 0)
            {
                ((RadioButton)dlPhotos.Items[userPreference.PhotoPref].FindControl("rbPhoto")).Checked = true;

                // Also set the hidden tracking value used for Javascript actions
                hidRbTrack.Value = userPreference.PhotoPref.ToString();
            }
        }

    }

    protected void btnUploadPhoto_Click(object sender, EventArgs e)
    {
        if (IsPostBack)
        {
            ProcessPhotoUpload();
            this.pnlPhotoPopup.Visible = true;
        }
    }

    protected void btnSaveClose_Click(object sender, EventArgs e)
    {

        if (hidRbTrack.Value == "") { return; }

        int imgSelected = System.Convert.ToInt32(hidRbTrack.Value);

        // Set the user's preference
        _userBL.SetUserPreferences(_personId, "PhotoPreference", imgSelected.ToString());

        SetPhotoSelection();

        // Reset the upload control
        FileUpload1.Reset();

        // Reset the displayed image
        EditPhotoSetup();
    }

    protected void btnClose_Click(object sender, EventArgs e)
    {
        // Reset the upload control
        FileUpload1.Reset();

        SetPhotoSelection();
    }

    protected void dlPhotos_OnLoad(object sender, EventArgs e)
    {
        dlPhotos.DataSource = _userBL.GetUserPhotoList(_personId, 2);
        dlPhotos.DataBind();

        if (!IsPostBack)
            SetPhotoSelection();
    }

    protected void rbPhoto_DataBinding(object sender, EventArgs e)
    {
        ((RadioButton)sender).Text = "System Photo " + _photoCount.ToString();
        ((RadioButton)sender).Attributes.Add("OnClick", string.Format("exclusiveCheckbox('{0}')", _photoCount - 1));

        _photoCount++;
    }

    protected void imgPhoto_DataBinding(object sender, EventArgs e)
    {
        int imgPhotoCount = _photoCount - 1;
        ((Image)sender).AlternateText = "System Photo " + imgPhotoCount.ToString();
        ((Image)sender).Attributes.Add("OnClick", string.Format("exclusiveCheckbox('{0}');", imgPhotoCount - 1));
    }

    protected void rbPhotoCustom_Load(object sender, EventArgs e)
    {
        ((RadioButton)sender).Attributes.Add("OnClick", "exclusiveCheckbox('9')");
    }

    protected void rbPhoto_CheckedChanged(object sender, EventArgs e)
    {

    }

    #endregion
}
