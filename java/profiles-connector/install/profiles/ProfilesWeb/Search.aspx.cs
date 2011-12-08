/*  
 
    Copyright (c) 2008-2010 by the President and Fellows of Harvard College. All rights reserved.  
    Profiles Research Networking Software was developed under the supervision of Griffin M Weber, MD, PhD.,
    and Harvard Catalyst: The Harvard Clinical and Translational Science Center, with support from the 
    National Center for Research Resources and Harvard University.


    Code licensed under a BSD License. 
    For details, see: LICENSE.txt 
  
*/
using System;
using System.Collections.Generic;
using System.Data;
using System.Web.Script.Services;
using System.Web.Services;
using System.Web.UI;
using System.Web.UI.WebControls;
using Connects.Profiles.BusinessLogic;
using Connects.Profiles.Service.DataContracts;
using Connects.Profiles.Utility;
using Microsoft.Practices.EnterpriseLibrary.Data;
using System.Data.Common;

public partial class Search : BasePage
{
    protected int MaxVisible = 1;
    public int PageSize = 15;
    protected static int _personId = 0;
    protected int _totalSearchResults = 0;
    protected int _totalRowCount = 0;
    protected int _PageCount = 0;

    protected bool _pagejumpchange = false;
    protected bool _rebound = false;
    private Random _myRandom = new Random();

    // Profiles OpenSocial Extension by UCSF 
    public List<Int32> OS_personIds = null;
    public string OS_message = null;

    #region Page Events

    protected void Page_Load(object sender, EventArgs e)
    {
        if (!IsPostBack)
        {
            Session["LastSearchTotalRowCount"] = 0;
            // Set up the initial page
            this.SetInitialPage();

            // Process querystring parameters
            this.ProcessQueryStrings();

            Session["ProfileSearchRequestCriteriaList"] = new List<string>();

            // Profiles OpenSocial Extension by UCSF
            OpenSocialHelper os = SetOpenSocialHelper(Profile.UserId, -1, Page);
            os.RemovePubsubGadgetsWithoutData();
            GenerateOpensocialJavascipt();

            LoadPersonFilter();
        }
    }
    #endregion

    #region Initialize Dropdowns

    private void FillDropdownLists()
    {
        ddlDepartment.DataSource = new DepartmentBL().GetDepartments();
        ddlDepartment.DataTextField = "Department";
        ddlDepartment.DataBind();
        ddlDepartment.Items.Insert(0, new ListItem("--Select--"));
        ddlDepartment.Items[0].Selected = true;

        ddlInstitution.DataSource = new InstitutionBL().GetInstitutions();
        ddlInstitution.DataTextField = "InstitutionName";
        ddlInstitution.DataBind();
        ddlInstitution.Items.Insert(0, new ListItem("--Select--"));
        ddlInstitution.Items[0].Selected = true;

        ddlDivision.DataSource = new DivisionBL().GetDivisions();
        ddlDivision.DataTextField = "DivisionName";
        ddlDivision.DataBind();
        ddlDivision.Items.Insert(0, new ListItem("--Select--"));
        ddlDivision.Items[0].Selected = true;

        ddlFacultyRank.DataSource = new FacultyBL().GetFacultyRanks();
        ddlFacultyRank.DataTextField = "FacultyRank";
        ddlFacultyRank.DataBind();
        ddlFacultyRank.Items.Insert(0, new ListItem("--Select--"));
        ddlFacultyRank.Items[0].Selected = true;
    }

    #endregion

    #region Button Events

    protected void btnSearch_Click(object sender, EventArgs e)
    {
        ProcessProfileSearch();
    }

    protected void lnkShowHelpText_Click(object sender, EventArgs e)
    {
        if (lnkShowHelpText.Text == "Explain search options")
        {
            lnkShowHelpText.Text = "Hide description of search options";
            phShowHelpText.Visible = true;
        }
        else
        {
            lnkShowHelpText.Text = "Explain search options";
            phShowHelpText.Visible = false;
        }
        //ReFill Spotlights
        GetMostViewedtoday(Profile.UserId);
        GetMostVeiwedThisMonth(Profile.UserId);
    }

    protected void btnSearchReset_Click(object sender, EventArgs e)
    {
        ResetSearch();
    }


    protected void lnkPrevPage_OnClick(object sender, EventArgs e)
    {
        _pagejumpchange = true;

    }
    protected void lnkNextPage_OnClick(object sender, EventArgs e)
    {
        _pagejumpchange = true;

    }


    #endregion

    #region Search Grid Events

    protected void grdSearchResults_RowDataBound(Object sender, GridViewRowEventArgs e)
    {
        if (e.Row.RowType != DataControlRowType.DataRow) return;

        ////set row hover 
        GridView gv = (GridView)sender;

        string name = "";
        string inst = "";
        string dept = "";
        string div = "";
        string title = "";
        string rank = "";

        if (((Person)e.Row.DataItem).AffiliationList != null)
        {
            AffiliationPerson aff = ((Person)e.Row.DataItem).AffiliationList.Affiliation[0];

            name = String.Format("{0} {1}", ((Person)e.Row.DataItem).Name.FirstName, ((Person)e.Row.DataItem).Name.LastName.Replace("'", ""));
            inst = aff.InstitutionName == null ? "" : aff.InstitutionName.Replace("'", "");
            dept = aff.DepartmentName == null ? "" : aff.DepartmentName.Replace("'", "");
            div = aff.DivisionName == null ? "" : aff.DivisionName.Replace("'", "");
            title = aff.JobTitle == null ? "" : aff.JobTitle.Replace("'", "");
            rank = aff.FacultyType == null ? "" : aff.FacultyType.Replace("'", "");
        }

        string hoverFunc = String.Format("doPersonOver('{0}','{1}','{2}','{3}','{4}','{5}','{6}');", name, inst, dept, div, title, rank, "");
        string className = (e.Row.RowState == DataControlRowState.Alternate) ? gv.AlternatingRowStyle.CssClass : gv.RowStyle.CssClass;
        e.Row.Attributes.Add("onmouseover", "this.className='gridHover';" + hoverFunc);
        e.Row.Attributes.Add("onmouseout", "this.className='" + className + "';doPersonOut()");

        // Set each cell to be clickable.  Avoid the last (hidden) column
        for (int i = 0; i < e.Row.Cells.Count - 2; i++)
        {
            e.Row.Cells[i].Attributes.Add("onclick", String.Format("document.location = 'ProfileDetails.aspx?From=SE&Person={0}'", gv.DataKeys[e.Row.RowIndex].Value.ToString()));
        }

    }

    protected void grdSearchResults_PageIndexChanged(object sender, EventArgs e)
    {
        SetBackPageURL();
        Session["ProfileSearchResultsPage"] = ((GridView)sender).PageIndex;
    }

    protected void grdSearchResults_DataBound(object sender, EventArgs e)
    {
        SetGridColumns();

        if (((GridView)sender).BottomPagerRow != null)
            ((GridView)sender).BottomPagerRow.Visible = true;
    }

    protected void lstSearchKeywordDisplay_ItemDataBound(object sender, RepeaterItemEventArgs e)
    {
        if ((e.Item.ItemType == ListItemType.Item) || ((e).Item.ItemType == ListItemType.AlternatingItem))
        {
            DataList dlKeywords = ((DataList)(e).Item.FindControl("lstKeywordMatchMeshHeader"));

            if (dlKeywords != null)
            {
                dlKeywords.DataSource = ((MatchingKeyword)(e).Item.DataItem).MatchingMeshHeader;
                dlKeywords.DataBind();
            }
        }

        if ((e).Item.ItemType == ListItemType.Footer)
        {
            try
            {
                Panel direct = ((Panel)(e).Item.FindControl("divDirect"));
                if (ConfigUtil.GetConfigItem("DirectServiceURL") != null)
                    direct.Visible = true;
            }
            catch (Exception ex) { }
        }



    }

    protected void txtPageJump_DataBinding(object sender, EventArgs e)
    {
        ((TextBox)sender).Text = (grdSearchResults.PageIndex + 1).ToString();
    }

    protected void txtPageJump_TextChanged(object sender, EventArgs e)
    {
        int newPage = 0;

        //RegularExpressionValidator pageValNum = ((RegularExpressionValidator)FindControlRecursive(Page, "valPageNum"));
        //RangeValidator pageValRange = ((RangeValidator)FindControlRecursive(Page, "valPagNumRange"));
        //CustomValidator pagerValidate = ((CustomValidator)FindControlRecursive(Page, "CustomValidator1"));
        //pagerValidate.Validate();

        //Page.Validate();
        //if (!Page.IsValid)
        //{
        //    if (!pageValNum.IsValid)
        //    {
        //        ((TextBox)sender).Text = "1";
        //        return;
        //    }
        //    else if (!pageValRange.IsValid)
        //    {
        //        ((TextBox)sender).Text = grdSearchResults.PageCount.ToString();
        //        return;
        //    }
        //}

        if (Int32.TryParse(((TextBox)sender).Text, out newPage))
        {
            // Subtract one since the display is not base 0
            //newPage = Convert.ToInt32(((TextBox)sender).Text) - 1;
            newPage--;

            int maxPage = grdSearchResults.PageCount - 1;

            if (newPage < 0)
                newPage = 0;
            else if (newPage > maxPage)
                newPage = maxPage;

            grdSearchResults.PageIndex = newPage;

            //if ((newPage >= 0) && (newPage <= grdSearchResults.PageCount))

            Session["ProfileSearchResultsPage"] = newPage;
            _pagejumpchange = true;
        }
    }

    protected void pageNumberValidate(object sender, ServerValidateEventArgs e)
    {
        return;

        int pageNum;
        TextBox txtPage = ((TextBox)FindControlRecursive(grdSearchResults, "txtPageJump"));
        Label lblPageErr = ((Label)FindControlRecursive(grdSearchResults, "lblPageError"));
        string errText = "Please enter valid page number";

        e.IsValid = false;

        if (e.Value != null)
        {
            if (!BaseCompareValidator.CanConvert(e.Value, ValidationDataType.Integer))
            {
                txtPage.Text = "1";
            }
            else
            {
                pageNum = Convert.ToInt32(e.Value);

                if (pageNum <= grdSearchResults.PageCount)
                {
                    e.IsValid = true;
                }
                else
                    txtPage.Text = grdSearchResults.PageCount.ToString();
            }
        }
        else
        {
            txtPage.Text = "1";
        }

        return;
    }

    protected void lblPageCount_DataBinding(object sender, EventArgs e)
    {
        ((Label)sender).Text = grdSearchResults.PageCount.ToString();
    }

    #endregion

    #region profilesObjDataSource Events

    protected void profilesObjDataSource_OnSelected(object sender, ObjectDataSourceStatusEventArgs e)
    {
        _totalRowCount = Convert.ToInt32(e.OutputParameters["totalCount"]);

        // Use the return value of the object data source's selection action to calculate the
        // number of records returned.  In this case we are dealing with a List<Person>.
        if (e.ReturnValue != null)
        {
            if (ViewState["ltrHeaderText"] != null)
            {
                ltHeader.Text = string.Format("Search Results ({0}) for " + ViewState["ltrHeaderText"].ToString(), _totalRowCount.ToString());
            }
            else
            {
                if (_totalRowCount > 0)
                {
                    ltHeader.Text = string.Format("Search Results ({0})", _totalRowCount);
                }
            }

            Cache[profilesObjDataSource.CacheKeyDependency] = new object();

            // Read the QueryId returned and modify the current search request to include this
            ((Profiles)Session["ProfileSearchRequest"]).QueryDefinition.QueryID = Convert.ToString(e.OutputParameters["queryID"]);

            // Profiles OpenSocial Extension by UCSF
            OS_message = null;
            OS_personIds = new List<Int32>();
            OpenSocialHelper os = SetOpenSocialHelper(Profile.UserId, -1, Page);
            if (OpenSocial().IsVisible() && OpenSocial().HasGadgetListeningTo(OpenSocialHelper.JSON_PERSONID_CHANNEL))
            {
                IDataReader reader = null;
                try
                {
                    Database db = DatabaseFactory.CreateDatabase();

                    string sqlCommand = "select personid from api_query_results where QueryID = '" + Convert.ToString(e.OutputParameters["queryID"]) + "';";
                    DbCommand dbCommand = db.GetSqlStringCommand(sqlCommand);
                    reader = db.ExecuteReader(dbCommand);
                    while (reader.Read())
                    {
                        OS_personIds.Add((Int32)reader["Personid"]);
                    }
                    OS_message = "" + OS_personIds.Count + " Profiles found";
                }
                catch (Exception ex)
                {
                    OS_message = "Error : " + ex.Message;
                }
                finally
                {
                    if (reader != null)
                    {
                        reader.Close();
                    }
                }
            }
            Session["OpenSocialJSONPersonIds"] = this.GetJSONPersonIds();
            OpenSocial().SetPubsubData(OpenSocialHelper.JSON_PERSONID_CHANNEL, this.GetJSONPersonIds());
            GenerateOpensocialJavascipt();
            // END Profiles OpenSocial Extension by UCSF

            Session["LastSearchTotalRowCount"] = _totalRowCount;
        }
    }

    protected void profilesObjDataSource_Selecting(object sender, ObjectDataSourceSelectingEventArgs e)
    {
        int startRecord = 0;

        if (Session["ProfileSearchRequest"] == null)
            Response.Redirect("~/search.aspx");

        string maxRecords = Convert.ToString(Session["LastSearchPageSize"]);

        if (!String.IsNullOrEmpty(maxRecords))
            ((Profiles)Session["ProfileSearchRequest"]).OutputOptions.MaxRecords = maxRecords;
        else
            ((Profiles)Session["ProfileSearchRequest"]).OutputOptions.MaxRecords = PageSize.ToString();

        startRecord = (grdSearchResults.PageIndex * PageSize) + 1;
        ((Profiles)Session["ProfileSearchRequest"]).OutputOptions.StartRecord = startRecord.ToString();

        // Set the selection input parameter to the session variable holding the search request
        e.InputParameters["pq"] = ((Profiles)Session["ProfileSearchRequest"]);
        e.InputParameters["queryID"] = ((Profiles)Session["ProfileSearchRequest"]).QueryDefinition.QueryID;
        e.InputParameters["totalCount"] = 0;
    }

    #endregion

    #region SpotLight Section - MostViewedtoday

    /// <summary>
    /// Get most veiwed Today for spotlight
    /// </summary>
    /// <param name="UID"></param>
    private void GetMostViewedtoday(int userId)
    {
        rptMostViewedToday.DataSource = _userBL.GetMostViewedToday(userId);
        rptMostViewedToday.DataBind();
    }

    #endregion

    #region SpotLight Section - MostViewedMonth
    /// <summary>
    /// Get Most veiwed This Month for spotlight
    /// </summary>
    /// <param name="UID"></param>
    private void GetMostVeiwedThisMonth(int userId)
    {
        rptMostViewedMonth.DataSource = _userBL.GetMostViewedThisMonth(userId);
        rptMostViewedMonth.DataBind();
    }
    #endregion

    #region Search Request Functions
    private string HTMLEncode(string value)
    {

        return value;
    }
    /// <summary>
    /// Build the search request, submit the search and process the result.
    /// This is the handler for the main search using the full set of criteria
    /// </summary>
    private void ProcessProfileSearch()
    {
        Profiles searchReq = ProfileHelper.GetNewProfilesDefinition();

        string lastName = this.HTMLEncode(txtLastName.Text.Trim());
        string firstName = this.HTMLEncode(txtFirstName.Text.Trim());

        Session["ProfileSearchRequestCriteriaList"] = new List<string>();
        Session["ProfileSearchRequestKeywordList"] = new List<string>();

        // Reset current page number
        Session["ProfileSearchResultsPage"] = 0;

        grdSearchResults.DataSource = null;

        // Name
        if (lastName.Length > 0)
        {
            searchReq.QueryDefinition.Name.LastName.Text = lastName.Trim();
            ((List<string>)Session["ProfileSearchRequestCriteriaList"]).Add(lastName);
        }

        if (firstName.Length > 0)
        {
            searchReq.QueryDefinition.Name.FirstName.Text = firstName.Trim();
            ((List<string>)Session["ProfileSearchRequestCriteriaList"]).Add(firstName);
        }

        // Institution Selection
        if (ddlInstitution.SelectedIndex != 0)
        {
            AffiliationInstitutionName affilInstname = new AffiliationInstitutionName();
            affilInstname.Text = ddlInstitution.SelectedItem.Text;
            searchReq.QueryDefinition.AffiliationList.Affiliation[0].InstitutionName = affilInstname;

            // Set the appropriate message on the right 
            ((List<string>)Session["ProfileSearchRequestCriteriaList"]).Add(chkInstitution.Checked == true ? "All except " + affilInstname.Text : affilInstname.Text);

            searchReq.QueryDefinition.AffiliationList.Affiliation[0].InstitutionName.Exclude = chkInstitution.Checked;
        }

        // Division Selection
        if (ddlDivision.SelectedIndex != 0)
        {
            AffiliationDivisionName affilDivname = new AffiliationDivisionName();
            affilDivname.Text = ddlDivision.SelectedItem.Text;
            searchReq.QueryDefinition.AffiliationList.Affiliation[0].DivisionName = affilDivname;

            // Set the appropriate message on the right 
            ((List<string>)Session["ProfileSearchRequestCriteriaList"]).Add(chkInstitution.Checked == true ? "All except " + affilDivname.Text : affilDivname.Text);

            searchReq.QueryDefinition.AffiliationList.Affiliation[0].DivisionName.Exclude = chkDivision.Checked;
        }


        // Department Selection
        if (ddlDepartment.SelectedIndex != 0)
        {
            AffiliationDepartmentName affilDeptname = new AffiliationDepartmentName();
            affilDeptname.Text = ddlDepartment.SelectedItem.Text;
            searchReq.QueryDefinition.AffiliationList.Affiliation[0].DepartmentName = affilDeptname;

            // Set the appropriate message on the right 
            ((List<string>)Session["ProfileSearchRequestCriteriaList"]).Add(chkDepartment.Checked == true ? "All except " + affilDeptname.Text : affilDeptname.Text);

            searchReq.QueryDefinition.AffiliationList.Affiliation[0].DepartmentName.Exclude = chkDepartment.Checked;
        }

        // Faculty Selection
        if (ddlFacultyRank.SelectedIndex != 0)
        {

            FacultyRankList ftList = new FacultyRankList();

            searchReq.QueryDefinition.FacultyRankList = new FacultyRankList();
            searchReq.QueryDefinition.FacultyRankList.FacultyRank = new List<string>();

            searchReq.QueryDefinition.FacultyRankList.FacultyRank.Add(ddlFacultyRank.SelectedItem.Value.ToString());

            ((List<string>)Session["ProfileSearchRequestCriteriaList"]).Add(ddlFacultyRank.SelectedItem.Text);
        }

        // Person Filter Section
        HiddenField hdnSelectedText = ((HiddenField)ctcFirst.FindControl("hdnSelectedText"));
        PersonFilter pf;
        searchReq.QueryDefinition.PersonFilterList = new PersonFilterList();

        if ((hdnSelectedText.Value.Length > 0) && (hdnSelectedText.Value != "--Select--"))
        {
            string[] pfArray = hdnSelectedText.Value.Split(',');

            for (int i = 0; i < pfArray.Length; i++)
            {
                pf = new PersonFilter();
                pf.Text = pfArray[i];

                searchReq.QueryDefinition.PersonFilterList.Add(pf);

                ((List<string>)Session["ProfileSearchRequestCriteriaList"]).Add(pfArray[i]);
            }
        }

        // Keywords
        if (txtKeyword.Text.Trim().Length > 0)
        {
            string keywordString;

            if (chkKeyword.Checked == true)
            {
                keywordString = this.HTMLEncode(txtKeyword.Text.Trim().Replace("\"", ""));

                searchReq.QueryDefinition.Keywords.KeywordString.Text = keywordString;
                searchReq.QueryDefinition.Keywords.KeywordString.MatchType = KeywordMatchType.exact;
                ((List<string>)Session["ProfileSearchRequestKeywordList"]).Add(keywordString);
            }
            else
            {
                keywordString = this.HTMLEncode(txtKeyword.Text.Trim());

                searchReq.QueryDefinition.Keywords.KeywordString.Text = keywordString;


                ((List<string>)Session["ProfileSearchRequestKeywordList"]).Add(keywordString);
            }
        }

        searchReq.Version = 2;

        // Save the search request into a session object so that the 
        // object data source can use it during the Select event


        Session["ProfileSearchRequest"] = searchReq;

        Session["MeshSearch"] = 0;
        Session["SearchPageIndex"] = 0;
        Session["WasSearchRun"] = "Y";

        // Get results bound to grid
        BindSearchResults(0);

        SetBackPageURL();
    }

    /// <summary>
    /// Build the search request, submit the search and process the result
    /// This is the handler for the "Mini Search" user control.
    /// </summary>
    private void ProcessMiniSearch()
    {
        Profiles profiles = ProfileHelper.GetNewProfilesDefinition();
        Session["ProfileSearchRequestCriteriaList"] = new List<string>();
        Session["ProfileSearchRequestKeywordList"] = new List<string>();

        // Name
        if (Request.QueryString["Lname"] != null)
        {
            string lastName = this.HTMLEncode(Request.QueryString["Lname"].ToString().Trim());
            profiles.QueryDefinition.Name.LastName.Text = lastName;
            ((List<string>)Session["ProfileSearchRequestCriteriaList"]).Add(lastName);
        }

        // Institution Selection
        if (Request.QueryString["Institute"] != null)
        {
            string inst = this.HTMLEncode(Request.QueryString["Institute"].ToString().Trim());

            AffiliationInstitutionName affilInstname = new AffiliationInstitutionName();
            affilInstname.Text = inst;
            profiles.QueryDefinition.AffiliationList.Affiliation[0].InstitutionName = affilInstname;            
            ((List<string>)Session["ProfileSearchRequestCriteriaList"]).Add(inst);
        }
        // Division Selection
        if (Request.QueryString["Division"] != null)
        {
            string inst = this.HTMLEncode(Request.QueryString["Division"].ToString().Trim());

            AffiliationDivisionName affilDivname = new AffiliationDivisionName();
            affilDivname.Text = inst;
            profiles.QueryDefinition.AffiliationList.Affiliation[0].DivisionName = affilDivname;
            ((List<string>)Session["ProfileSearchRequestCriteriaList"]).Add(inst);
        }
        // Department Selection
        if (Request.QueryString["DeptName"] != null)
        {
            AffiliationDepartmentName affilDeptname = new AffiliationDepartmentName();
            affilDeptname.Text = this.HTMLEncode(Request.QueryString["DeptName"].ToString().Trim());
            profiles.QueryDefinition.AffiliationList.Affiliation[0].DepartmentName = affilDeptname;
            ((List<string>)Session["ProfileSearchRequestCriteriaList"]).Add(affilDeptname.Text);
        }

        // Keywords
        if (Request.QueryString["Keyword"] != null)
        {
            string keyword = this.HTMLEncode(Request.QueryString["Keyword"].ToString().Trim());

            profiles.QueryDefinition.Keywords.KeywordString.Text = keyword;
            ((List<string>)Session["ProfileSearchRequestKeywordList"]).Add(keyword);
        }

        profiles.Version = 2;

        // Save the search request into a session object so that the 
        // object data source can use it during the Select event
        Session["ProfileSearchRequest"] = profiles;


        Session["MeshSearch"] = 0;
        Session["WasSearchRun"] = "Y";

        BindSearchResults(0);
    }

    /// <summary>
    /// Build the search request, submit the search and process the result.
    /// This is the handler for the Keyword searches
    /// </summary>
    private void ProcessKeywordSearch()
    {
        Session["ProfileSearchRequestCriteriaList"] = new List<string>();
        Session["ProfileSearchRequestKeywordList"] = new List<string>();

        Profiles profiles = ProfileHelper.GetNewProfilesDefinition();


        string keywordString = this.HTMLEncode(Convert.ToString(Request.QueryString["Word"].ToString().Trim()));

        // Keywords
        if (keywordString.Length > 0)
        {
            profiles.QueryDefinition.Keywords.KeywordString.Text = keywordString;
            //if there is a space char in the string, then its a group of words coming from the Most Viewed..
            if (keywordString.Trim().Contains(" "))
            {
                profiles.QueryDefinition.Keywords.KeywordString.MatchType = KeywordMatchType.exact;
            }
            // Add the searched keyword to the right side of the page
            ((List<string>)Session["ProfileSearchRequestKeywordList"]).Add(keywordString);
        }

        profiles.Version = 2;

        // Save the search request into a session object so that the 
        // object data source can use it during the Select event
        Session["ProfileSearchRequest"] = profiles;

        Session["MeshSearch"] = 0;
        Session["WasSearchRun"] = "Y";

        // Get results bound to grid
        BindSearchResults(0);
    }

    /// <summary>
    /// Build the search request, submit the search and process the result
    /// This is the handler for the Department searches
    /// </summary>
    private void ProcessDeptSearch()
    {
        Session["ProfileSearchRequestCriteriaList"] = new List<string>();
        Session["ProfileSearchRequestKeywordList"] = new List<string>();

        Profiles profiles = ProfileHelper.GetNewProfilesDefinition();

        // Name
        profiles.QueryDefinition.Name.LastName.Text = this.HTMLEncode(txtLastName.Text.Trim());
        profiles.QueryDefinition.Name.FirstName.Text = this.HTMLEncode(txtFirstName.Text.Trim());

        // Institution Selection
        if (Request.QueryString["InstName"] != null)
        {
            AffiliationInstitutionName affilInstname = new AffiliationInstitutionName();
            affilInstname.Text = this.HTMLEncode(Request.QueryString["InstName"].ToString().Trim());
            profiles.QueryDefinition.AffiliationList.Affiliation[0].InstitutionName = affilInstname;

            ((List<string>)Session["ProfileSearchRequestCriteriaList"]).Add(affilInstname.Text);
        }

        // Department Selection
        if (Request.QueryString["DeptName"] != null)
        {
            AffiliationDepartmentName affilDeptname = new AffiliationDepartmentName();
            affilDeptname.Text = this.HTMLEncode(Request.QueryString["DeptName"].ToString().Trim());
            profiles.QueryDefinition.AffiliationList.Affiliation[0].DepartmentName = affilDeptname;

            ((List<string>)Session["ProfileSearchRequestCriteriaList"]).Add(affilDeptname.Text);
        }

        profiles.Version = 2;

        // Save the search request into a session object so that the 
        // object data source can use it during the Select event
        Session["ProfileSearchRequest"] = profiles;

        Session["MeshSearch"] = 0;
        Session["WasSearchRun"] = "Y";

        _personId = GetPersonFromQueryString();
        lnkBackToProfile.NavigateUrl = "~/ProfileDetails.aspx?Person=" + _personId.ToString();
        lnkBackToProfile.Visible = true;

        BindSearchResults(0);
    }

    private void ResetSearch()
    {
        txtLastName.Text = "";
        txtFirstName.Text = "";
        ddlInstitution.SelectedIndex = -1;
        ddlDepartment.SelectedIndex = -1;
        txtKeyword.Text = "";

        chkDepartment.Checked = false;
        chkInstitution.Checked = false;
        chkKeyword.Checked = false;
        ddlFacultyRank.SelectedIndex = -1;
        LoadPersonFilter();

        LtMsg.Text = "";

        GetMostViewedtoday(Profile.UserId);
        GetMostVeiwedThisMonth(Profile.UserId);

        Session["ProfileSearchRequest"] = null;
        Session["ProfileSearchRequestCriteriaList"] = new List<string>();
        Session["ProfileSearchRequestKeywordList"] = new List<string>();
    }

    #endregion

    #region Properties
    private string getSortDirectionString(SortDirection sortDireciton)
    {
        string newSortDirection = String.Empty;
        if (sortDireciton == SortDirection.Ascending)
        {
            newSortDirection = "ASC";
        }
        else
        {
            newSortDirection = "DESC";
        }

        return newSortDirection;
    }
    #endregion

    #region Private Functions

    private void BindSearchResults(int initialPage)
    {
        LtMsg.Visible = false;
        LtMsg.Text = "";

        // Be sure to kill the ObjectDataSource cache
        Cache.Remove(profilesObjDataSource.CacheKeyDependency);

        // Tell the grid to bind the data
        grdSearchResults.DataBind();

        if (grdSearchResults.BottomPagerRow != null)
            grdSearchResults.BottomPagerRow.Visible = true;

        if (Session["LastSearchPageSize"] != null)
            this.grdSearchResults.PageSize = Convert.ToInt32(Session["LastSearchPageSize"]);

        // Set the initial page
        this.grdSearchResults.PageIndex = System.Convert.ToInt32(Session["ProfileSearchResultsPage"]);

        // Profiles OpenSocial Extension by UCSF
        OpenSocialHelper os = SetOpenSocialHelper(Profile.UserId, -1, Page);
        os.SetPubsubData(OpenSocialHelper.JSON_PERSONID_CHANNEL, this.GetJSONPersonIds());
        os.RemoveGadget("Activities");  // do not show Activities gadget on search results page
        GenerateOpensocialJavascipt();
        pnlOpenSocialGadgets.Visible = OpenSocial().IsVisible();

        if (grdSearchResults.Rows.Count == 0 && initialPage > -1)
        {
            LtMsg.Visible = true;
            LtMsg.Text = "<span style='color:#990000; font-weight:bold'>" +
                (OpenSocial() != null && GetKeyword() != null && OpenSocial().IsVisible() && GetKeyword().Length > 0 && !HasAdditionalSearchCriteria() ?
                "No keyword matches found.  Please check Full Text Search results below." :
                "No matching people could be found. Please check your entry and try again.") + "</span>";

            // Kill the session variable used to bind the results grid
            Session["ProfileSearchRequest"] = null;

            Session["ProfileSearchResultsPage"] = 0;
        }
        else
        {
            if ((grdSearchResults.Rows.Count < this.grdSearchResults.PageSize) && this.grdSearchResults.PageIndex == 0 && !_rebound && NotFromQuickSearch() && Request["From"] == null)
            {
                _rebound = true;


                try { if (Request["page"].ToString() != "0") { ProcessProfileSearch(); } }
                catch { ProcessProfileSearch(); }


            }

            _rebound = false;

            pnlSearchResults.Visible = true;
            pnlSearch.Visible = false;
            divSpotlight.Visible = false;
            divSearchCriteria.Visible = true;

            ShowRightColumn();
            ShowSearchCriteria();

            searchResultsPersonSummaryContainer.Visible = true;
        }

        Session["BackPage"] = String.Format("~/Search.aspx?page={0}", Convert.ToString(Session["ProfileSearchResultsPage"]));
        //lnkSelectSort.Text = "Sort By " + GetSortTextFromEnum(((Profiles)Session["ProfileSearchRequest"]).OutputOptions.SortType);
    }

    private void ShowSearchCriteria()
    {
        List<string> searchRequestList = (List<string>)Session["ProfileSearchRequestCriteriaList"];
        if (searchRequestList.Count > 0)
        {
            lstSearchCriteriaDisplay.DataSource = (List<string>)Session["ProfileSearchRequestCriteriaList"];
            lstSearchCriteriaDisplay.DataBind();
        }

        string keyword = "";

        if (this.txtKeyword.Text.Length > 0)
            keyword = this.txtKeyword.Text;
        else if (Session["ProfileSearchRequestKeywordList"] != null)
        {
            if (((List<string>)Session["ProfileSearchRequestKeywordList"]).Count > 0)
                keyword = ((List<string>)Session["ProfileSearchRequestKeywordList"])[0];
        }

        if (keyword.Length > 0)
        {
            Profiles searchReq = (Profiles)Session["ProfileSearchRequest"];
            string searchKeyword = searchReq.QueryDefinition.Keywords.KeywordString.Text;
            string queryId = searchReq.QueryDefinition.QueryID;
            KeywordMatchType matchType = searchReq.QueryDefinition.Keywords.KeywordString.MatchType;
            bool matchExact = (matchType == KeywordMatchType.exact) ? true : false;

            MatchingKeywordList keywordList = new Connects.Profiles.Service.ServiceImplementation.ProfileService().GetMatchingKeywords(searchKeyword, queryId, matchExact);

            if (keywordList.Count > 0)
            {
                lstSearchKeywordDisplay.DataSource = keywordList;
                lstSearchKeywordDisplay.DataBind();
            }
        }

    }

    public string GetKeyword()
    {
               
      

        string keyword = "";

        if (this.txtKeyword.Text.Length > 0)
            keyword = this.txtKeyword.Text;
        else if (Session["ProfileSearchRequestKeywordList"] != null)
        {
            if (((List<string>)Session["ProfileSearchRequestKeywordList"]).Count > 0)
                keyword = ((List<string>)Session["ProfileSearchRequestKeywordList"])[0];
        }

      
        return keyword;
    }

    private void ShowMiniSearch(bool visible)
    {
        //Find Control in Masterpage
        ShowControl("upnlMinisearch", visible);

        // Refresh the update panel
        RefreshUpdatePanel("upnlMinisearch");
    }

    private void SetInitialPage()
    {
        //Fill Spotlights
        GetMostViewedtoday(Profile.UserId);
        GetMostVeiwedThisMonth(Profile.UserId);

        Session["PersonIsMy"] = "This Person";

        FillDropdownLists();
        ltHeader.Text = "Search";

        // Hide min-search section
        ((System.Web.UI.Control)Master.Master.FindControl("left").FindControl("divLeftTop")).Visible = false;

        // Make sure the right panel is hidden
        //HideRightColumn();

        // Hide institution if specified in web.config
        rowInstitution.Visible = !Convert.ToBoolean(ConfigUtil.GetConfigItem("HideInstitutionSelectionForSearch"));

        // Hide department if specified in web.config
        rowDepartment.Visible = !Convert.ToBoolean(ConfigUtil.GetConfigItem("HideDepartmentSelectionForSearch"));

        // Hide division if specified in web.config
        rowDivision.Visible = !Convert.ToBoolean(ConfigUtil.GetConfigItem("HideDivisionSelectionForSearch"));

        divSpotlight.Visible = true;
        divSearchCriteria.Visible = false;

        // Profiles OpenSocial Extension by UCSF
        pnlOpenSocialGadgets.Visible = false;

        // 
        searchResultsPersonSummaryContainer.Visible = false;
    }

    private bool NotFromQuickSearch()
    {
        bool _rtn = false;
        try
        {
            //If Query Comes from Search Page SpotLight Section & Master Page MiniSearch button Onclick event
            if (Request["From"].Equals("SP"))
            {
                _rtn = false;
            }
            else
            {
                _rtn = true;
            }
        }
        catch { _rtn = true; }
        return _rtn;
    }
    private void ProcessQueryStrings()
    {
        if (Request.QueryString["From"] != null)
        {
            //If Query Comes from Search Page SpotLight Section & Master Page MiniSearch button Onclick event
            if (!NotFromQuickSearch())
            {
                this.ProcessMiniSearch();
            }

            //If Query Comes from Grid MeSH keyword Section then populate results for that keyword
            if (Request["Word"] != null)
            {
                this.ProcessKeywordSearch();
            }
            else if (Request["From"].Equals("PD"))
            {
                Session["WasSearchRun"] = "";
                Session["BackPage"] = "~/Search.aspx";
            }
            else if (Request["From"].Equals("dept"))
            {
                this.ProcessDeptSearch();
            }
        }

        // Processing of links back to previous search results.
        if (Request.QueryString["page"] != null)
        {
            if (((string)Session["WasSearchRun"] == "Y") && (Session["ProfileSearchRequest"] != null))
            {
                // Temporary fix PRG
                ((Profiles)Session["ProfileSearchRequest"]).QueryDefinition.PersonID = null;
                BindSearchResults(Convert.ToInt32((string)Request.QueryString["page"]));
            }
        }
        else
            Session["BackPage"] = "~/Search.aspx";


        SetBackPageURL();
    }

    #endregion

    #region Search Results Control Events
    protected void lstSelectCol_SelectedIndexChanged(object sender, EventArgs e)
    {
        grdSearchResults.DataSource = null;
        BindSearchResults(0);
    }

    protected void lstPageSize_SelectedIndexChanged(object sender, EventArgs e)
    {
        //This still default var is used just to defeat the firing of this event when the next or previous buttons are used.
        //_stillDefault along with the _pagejumpchange flags help ensure this event is only dealing with when
        // the lstPageSize event fires and only it fires.
        bool _stillDefault = false;

        if (Convert.ToInt32(Session["LastSearchPageSize"]) == Convert.ToInt32(((ListBox)sender).SelectedValue))
        {
            return;
        }

        if (Convert.ToInt32(((ListBox)sender).SelectedValue) == PageSize)
        {
            _stillDefault = true;
        }

        Session["LastSearchPageSize"] = Convert.ToInt32(((ListBox)sender).SelectedValue);
        grdSearchResults.PageSize = Convert.ToInt32(((ListBox)sender).SelectedValue);

        if (!_pagejumpchange && !_stillDefault)
        {
            Session["ProfileSearchResultsPage"] = 0;  //reset this if the page size dropdown changes
        }

        _pagejumpchange = false;



        BindSearchResults(0);
    }

    protected void lstPageSize_Load(object sender, EventArgs e)
    {
        if (Session["LastSearchPageSize"] != null)
            ((ListBox)sender).SelectedValue = Session["LastSearchPageSize"].ToString();
        else
            ((ListBox)sender).SelectedValue = "15";




    }

    protected void valPagNumRange_DataBinding(object sender, EventArgs e)
    {
        ((RangeValidator)sender).MaximumValue = grdSearchResults.PageCount.ToString();
    }

    protected void lblPageCounter_Load(object sender, EventArgs e)
    {
        int startRow = (grdSearchResults.PageIndex * grdSearchResults.PageSize) + 1;
        int endRow = startRow + grdSearchResults.PageSize - 1;

        _totalRowCount = Convert.ToInt32(Session["LastSearchTotalRowCount"]);

        // Make sure we don't exceed the total row count
        if (endRow > _totalRowCount)
            endRow = _totalRowCount;

        Label pageCounter = (Label)sender;
        pageCounter.Text = "&nbsp;" + "Record" + "&nbsp;" + startRow.ToString() + "&nbsp;-&nbsp;" + endRow.ToString() + "&nbsp;" + "of" + "&nbsp;" + _totalRowCount.ToString() + "&nbsp";
    }

    protected void lstColumns_SelectedIndexChanged(object sender, EventArgs e)
    {
        BindSearchResults(0);
    }

    #endregion

    #region Search Results Sorting

    protected void grdSearchResults_Sorting(object sender, GridViewSortEventArgs e)
    {
        string currentSort = lstSelectSort.SelectedValue;

        if (currentSort.Contains(","))
        {
            currentSort = currentSort.Split(',')[0];
        }


        if (e.SortExpression.Contains(","))
        {
            e.SortExpression = e.SortExpression.Split(',')[0];
        }



        if (e.SortExpression == currentSort)
        {
            SortResults(e.SortExpression + "_DESC", "");
            lstSelectSort.SelectedValue = e.SortExpression + "_DESC";
        }
        else
        {
            SortResults(e.SortExpression, "");
            lstSelectSort.SelectedValue = e.SortExpression;
        }

        //We have to do this or we will get an exception
        e.Cancel = true;
    }

    protected void lstSortBy_SelectedIndexChanged(object sender, EventArgs e)
    {
        string sortBy = ((ListBox)sender).SelectedValue;

        SortResults(sortBy, "");
    }

    private void SortResults(string sortBy, string sortDirection)
    {
        OutputOptionsSortType st = new OutputOptionsSortType();
        st = OutputOptionsSortType.LastFirstName;

        switch (sortBy)
        {
            case "Institution_Fullname":
                st = OutputOptionsSortType.Institution_Fullname;
                break;
            case "Institution_Fullname_DESC":
                st = OutputOptionsSortType.Institution_Fullname;
                break;
            case "Department":
                st = OutputOptionsSortType.Department;
                break;
            case "Department_DESC":
                st = OutputOptionsSortType.Department;
                break;
            case "Publications":
                st = OutputOptionsSortType.Publications;
                break;
            case "Publications_DESC":
                st = OutputOptionsSortType.Publications;
                break;
            case "LastFirstName":
                st = OutputOptionsSortType.LastFirstName;
                break;
            case "LastFirstName_DESC":
                st = OutputOptionsSortType.LastFirstName;
                break;
            case "QueryRelevance":
                st = OutputOptionsSortType.QueryRelevance;
                break;
            case "QueryRelevance_DESC":
                st = OutputOptionsSortType.QueryRelevance;
                break;
            case "FacultyRank":
                st = OutputOptionsSortType.FacultyRank;
                break;
            case "FacultyRank_DESC":
                st = OutputOptionsSortType.FacultyRank;
                break;
            case "PersonId":
                st = OutputOptionsSortType.PersonId;
                break;
            case "PersonId_DESC":
                st = OutputOptionsSortType.PersonId;
                break;
            case "Division":
                st = OutputOptionsSortType.Division;
                break;
            case "Division_DESC":
                st = OutputOptionsSortType.Division;
                break;
        }

        if (Session["ProfileSearchRequest"] == null) { return; }

        ((Profiles)Session["ProfileSearchRequest"]).OutputOptions.SortType = st;

        if (!sortBy.Contains("_DESC"))
            ((Profiles)Session["ProfileSearchRequest"]).OutputOptions.SortAsc = "True";
        else if (sortDirection == "Ascending")
            ((Profiles)Session["ProfileSearchRequest"]).OutputOptions.SortAsc = "True";
        else
            ((Profiles)Session["ProfileSearchRequest"]).OutputOptions.SortAsc = "False";

        grdSearchResults.DataSource = null;
        BindSearchResults(-1);
    }

    #endregion

    #region Grid Data Binding Helpers

    protected string GetInstitutionText(object affiliationList)
    {
        string text = "";

        if (((AffiliationListPerson)affiliationList).Affiliation != null)
        {
            if (((AffiliationListPerson)affiliationList).Affiliation.Count > 0)
                text = Convert.ToString(((AffiliationListPerson)affiliationList).Affiliation[0].InstitutionName);
        }

        return text;
    }

    protected string GetDepartmentText(object affiliationList)
    {
        string text = "";

        if (((AffiliationListPerson)affiliationList).Affiliation != null)
        {
            if (((AffiliationListPerson)affiliationList).Affiliation.Count > 0)
                text = Convert.ToString(((AffiliationListPerson)affiliationList).Affiliation[0].DepartmentName);
        }

        return text;
    }

    protected string GetDivisionText(object affiliationList)
    {
        string text = "";

        if (((AffiliationListPerson)affiliationList).Affiliation != null)
        {
            if (((AffiliationListPerson)affiliationList).Affiliation.Count > 0)
                text = Convert.ToString(((AffiliationListPerson)affiliationList).Affiliation[0].DivisionName);
        }

        return text;
    }

    protected string GetFacultyRank(object affiliationList)
    {
        string text = "";

        if (((AffiliationListPerson)affiliationList).Affiliation != null)
        {
            if (((AffiliationListPerson)affiliationList).Affiliation.Count > 0)
                text = Convert.ToString(((AffiliationListPerson)affiliationList).Affiliation[0].FacultyType);
        }

        return text;
    }

    #endregion

    #region Search Results Helper Functions

    protected void SetGridColumns()
    {
        bool needToRefreshColumns = true;

        if (lstColumns.Items.Count != 0)
        {
            needToRefreshColumns = false;
            Session["SEARCH_COLS"] = lstColumns;
        }
      

        //when selected column names count = 0  
        if (needToRefreshColumns)
        {
            //get Default selected columns from web.config in appSettings
            string l_defaultColumnsFromConfig = ConfigUtil.GetConfigItem("ProfileSearchDefaultColumns");


                ListBox lstcolumns = null;
                if (Session["SEARCH_COLS"] != null) {
                    lstcolumns = (ListBox)Session["SEARCH_COLS"];
                    foreach(ListItem item in lstcolumns.Items)
                    {
                        if (item.Selected)
                        {
                            if (!l_defaultColumnsFromConfig.Contains(item.Text)) {
                                l_defaultColumnsFromConfig = l_defaultColumnsFromConfig + "," + item.Text;
                            }
                        }
                    }
                    

                }




            string[] l_defaultColumnsArr = l_defaultColumnsFromConfig.Split(',');

            List<string> l_AllColumns = new List<string>();
            foreach (DataControlField l_GridColumn in grdSearchResults.Columns)
            {
                //using filtering only for not-Templated columns
                if (!string.IsNullOrEmpty(l_GridColumn.SortExpression))
                    l_AllColumns.Add(l_GridColumn.HeaderText);
            }

            //l_AllColumns.Remove("Division");

            //bind listBox
            
                lstColumns.Items.Clear();
                lstColumns.DataSource = l_AllColumns;
                lstColumns.DataBind();
            
            
                //check "default" listBoxItems 
                foreach (ListItem l_Item in lstColumns.Items)
                {
                    foreach (string columnName in l_defaultColumnsArr)
                    {
                        //after split operation <string.split(',')> the last element usually is space
                        //so we must validate each value
                        if (!string.IsNullOrEmpty(columnName))
                        {
                            if (columnName == l_Item.Value)
                            {
                                l_Item.Selected = true;
                            }
                        }
                    }

                    if (l_Item.Value == "Name")
                        l_Item.Enabled = false;
                }
            
        }

        for (int i = 1; i < grdSearchResults.Columns.Count; i++)
        {
            DataControlField l_Field = grdSearchResults.Columns[i];

            //when current DataControlField is aspTemplateField(Edit or Delete) then break
            if (string.IsNullOrEmpty(l_Field.SortExpression)) break;

            bool l_isVisibleColumn = false;

            foreach (ListItem l_ListItem in lstColumns.Items)
            {
                if (l_ListItem.Selected)
                {
                    if (l_ListItem.Value == l_Field.HeaderText)
                    {
                        l_isVisibleColumn = true;
                        break;
                    }
                }
            }

            l_Field.Visible = l_isVisibleColumn;
        }

        // Hide the Why column if we're not dealing with a keyword search
        if (Session["ProfileSearchRequestKeywordList"] != null)
        {
            if (((List<String>)Session["ProfileSearchRequestKeywordList"]).Count > 0)
                grdSearchResults.Columns[grdSearchResults.Columns.Count - 2].Visible = true;
            else
                grdSearchResults.Columns[grdSearchResults.Columns.Count - 2].Visible = false;
        }

    }

    /// <summary>
    /// Returns and integer representing the offset to apply to the popup control in the search results
    /// this handles the situation where the popup control renders too low on the screen and you 
    /// can't read the contents.  Switches it from rendering below the row to rending above the row.
    /// </summary>
    /// <param name="containerItemIndex"></param>
    /// <returns>int</returns>
    protected int ControlPopupOffset(int containerItemIndex)
    {
        int midPoint = Convert.ToInt32((this.grdSearchResults.PageSize / 2));

        if (containerItemIndex <= midPoint)
            return 0;
        else
            // Should refactor this to be dynamic..
            return -220;
    }


    /// <summary>
    /// Used to initialize the "checkox tree" control.  See usp_GetPersonTypes
    /// for the patter of data used to populate this control
    /// with the "group" and "detail" items.
    /// </summary>
    protected void LoadPersonFilter()
    {
        DataSet ds = new FacultyBL().GetPersonTypes();

        ctcFirst.DataMasterName = "DataMasterName";
        ctcFirst.DataDetailName = "DataDetailName";

        ctcFirst.DataMasterIDField = "personTypeGroupId";
        ctcFirst.DataMasterTextField = "personTypeGroup";

        ctcFirst.DataDetailIDField = "personTypeFlagId";
        ctcFirst.DataDetailTextField = "personTypeFlag";

        ctcFirst.DataSource = ds;
        ctcFirst.DataBind();
    }

    private void SetBackPageURL()
    {
        // Add page querystring variable to keep track of pagination in grid
        string uri = Request.Url.ToString();
        if (uri.IndexOf("page=") > -1)
        { uri = uri.Substring(0, uri.IndexOf("page=") - 1); }

        string pageValue = "";
        if (uri.IndexOf("?") > -1)
        { pageValue = "&page=" + grdSearchResults.PageIndex.ToString(); }
        else
        { pageValue = "?page=" + grdSearchResults.PageIndex.ToString(); }

        Session["BackPage"] = uri + pageValue;
    }

    public Int32 GetDefaultPageSize()
    {
        if (Session["LastSearchPageSize"] != null)
            return Convert.ToInt32(Session["LastSearchPageSize"]);
        else
            return PageSize;
    }

    public bool DisplayMatchingPubsLink()
    {
        bool show = false;

        if (((Profiles)Session["ProfileSearchRequest"]).QueryDefinition.Keywords.KeywordString.Text != null)
            show = true;

        return show;
    }

    #endregion

    // Profiles OpenSocial Extension by UCSF
    #region OpenSocial Helpers
    [System.Web.Services.WebMethod]
    public static string GetPublishedPeople(string queryID)
    {
        return queryID;
    }

    public string GetJSONPersonIds()
    {
        if (this.OS_message != null)
        {
            return OpenSocialHelper.BuildJSONPersonIds(OS_personIds, OS_message);
        }
        else if (Session["OpenSocialJSONPersonIds"] != null)
        {
            return (string)Session["OpenSocialJSONPersonIds"];
        }
        return "{}";
    }

    public bool HasAdditionalSearchCriteria()
    {
        return (Session["ProfileSearchRequestCriteriaList"] != null && ((List<string>)Session["ProfileSearchRequestCriteriaList"]).Count > 0) ||
            (Request.QueryString["Lname"] != null && Request.QueryString["Lname"].ToString().Trim().Length > 0) ||
            (Request.QueryString["Institute"] != null && Request.QueryString["Institute"].ToString().Trim().Length > 0) ||
            (Request.QueryString["DeptName"] != null && Request.QueryString["DeptName"].ToString().Trim().Length > 0) ||
            txtLastName.Text.Trim().Length > 0 || txtFirstName.Text.Trim().Length > 0 ||
            ddlInstitution.SelectedIndex != 0 || ddlDepartment.SelectedIndex != 0 || ddlFacultyRank.SelectedIndex != 0 ||
            (((HiddenField)ctcFirst.FindControl("hdnSelectedText")).Value.Length > 0 && ((HiddenField)ctcFirst.FindControl("hdnSelectedText")).Value != "--Select--");
    }
    #endregion


}
