<%@ Page Language="C#" MasterPageFile="~/ProfilesPage.master" CodeFile="Search.aspx.cs"
    ValidateRequest="true" Inherits="Search" Title="Profiles | Search" %>

<%@ Register Src="UserControls/ucProfileBaseInfo.ascx" TagName="ProfileBaseInfo"
    TagPrefix="ucProfileBaseInfo" %>
<%@ Register Src="UserControls/ucPassiveNetwork.ascx" TagName="PassiveNetwork" TagPrefix="ucPassiveNetwork" %>
<%@ Register Src="UserControls/ComboTreeCheck.ascx" TagName="ComboTreeCheck" TagPrefix="uc1" %>
<%@ Reference Control="UserControls/ucMiniSearch.ascx" %>
<%@ MasterType VirtualPath="~/ProfilesPage.master" %>
<%--
    Copyright (c) 2008-2010 by the President and Fellows of Harvard College. All rights reserved.  
    Profiles Research Networking Software was developed under the supervision of Griffin M Weber, MD, PhD.,
    and Harvard Catalyst: The Harvard Clinical and Translational Science Center, with support from the 
    National Center for Research Resources and Harvard University.


    Code licensed under a BSD License. 
    For details, see: LICENSE.txt 
 --%>
<asp:Content ID="Content4" ContentPlaceHolderID="HeadContentPlaceHolder2" runat="Server">
</asp:Content>
<asp:Content ID="Content3" ContentPlaceHolderID="HeadingContentPlaceHolder" runat="Server">
    <link rel="stylesheet" type="text/css" href="JQuery/ui.dropdownchecklist.css" />

    <script type="text/javascript" src="Scripts/comboTreeCheck.js"></script>

    <script type="text/javascript" src="JQuery/jquery.js"></script>

    <script type="text/javascript" src="JQuery/ui.core.js"></script>

    <script type="text/javascript" src="JQuery/ui.dropdownchecklist.js"></script>

</asp:Content>
<asp:Content ID="Content1" ContentPlaceHolderID="MiddleContentPlaceHolder" runat="Server">

    <script type="text/javascript">
        var lastPassiveDisplay = 0;

        function checkPageSelection() {
            var re3digit = /^\d{1,3}$/;
            var pageSel = document.getElementById("ctl00$ctl00$middle$MiddleContentPlaceHolder$grdSearchResults$ctl18$txtPageJump");

            if (pageSel.value.search(re3digit) == -1) //if match failed
            {
                alert("Please enter a valid 3 digit number")
                return false;
            }
        }

        function doPersonOver(n, i, d, v, t, f, q) {
            var s = '';
            if (n != '') { s += '<div class="label">Name</div>'; s += '<div class="data">' + n + '</div>'; }
            if (i != '') { s += '<div class="label">Institution</div>'; s += '<div class="data">' + i + '</div>'; }
            if (d != '') { s += '<div class="label">Department</div>'; s += '<div class="data">' + d + '</div>'; }
            if (v != '') { s += '<div class="label">Division</div>'; s += '<div class="data">' + v + '</div>'; }
            if (f != '') { s += '<div class="label">Faculty Rank</div>'; s += '<div class="data">' + f + '</div>'; }
            if (t != '') { s += '<div class="label">Title</div>'; s += '<div class="data">' + t + '</div>'; }
            if (q != '') { s += '<div class="label">Query Relevance</div>'; s += '<div class="data">' + q + '</div>'; s += '<div class="data" style="font-size:10px;">(Click Why for details.)</div>'; }
            // set html
            $("div[id='divPersonSummary']").html("Person Summary");
            var p = $("div[id*=searchResultsPersonSummaryContent]");
            p[0].innerHTML = s;
            var p = $("div[id*=searchResultsPersonSummary]");
            p[0].style.display = 'inline';
        }

        function doPersonOut() {
            var p = $("div[id*=searchResultsPersonSummary]");
            p[0].style.display = 'none';
        }

        
    </script>

    <script type="text/javascript">
        $(document).ready(function() {
            $("#<%=lstColumns.ClientID %>").dropdownchecklist({ maxDropHeight: 120, width: 100 });
            $("div[id='divPersonSummary']").html("");

        });
    </script>

    <table width="100%">
        <tr>
            <td>
                <div class="pageTitle">
                    <asp:Literal ID="ltHeader" runat="server" /></div>
            </td>
            <td>
                <div style="text-align: right; padding-right: 8px;">
                    <asp:HyperLink ID="lnkBackToProfile" CssClass="rtIndent8" runat="server" Visible="false">Back to Profile</asp:HyperLink></div>
            </td>
        </tr>
    </table>
    <asp:Panel ID="pnlSearch" runat="server" DefaultButton="btnSearch">
        <div class="searchForm">
            <div class="searchSection">
                <table>
                    <tr>
                        <th style="text-align: right;">
                            Keyword
                        </th>
                        <td class="fieldMain">
                            <asp:TextBox ID="txtKeyword" runat="server" Width="250px" ToolTip="Enter Publication Keywords"
                                AutoCompleteType="None" />
                        </td>
                        <td class="fieldOptions">
                            <asp:CheckBox ID="chkKeyword" runat="server" />&nbsp;<span style="color: #666666;">Search
                                for exact phrase</span>
                        </td>
                    </tr>
                </table>
            </div>
            <div class="searchSection">
                <table>
                    <tr>
                        <th>
                            Last Name
                        </th>
                        <td class="fieldMain">
                            <asp:TextBox ID="txtLastName" runat="server" Width="250px" ToolTip="Enter LastName" />
                        </td>
                        <td>
                            &nbsp;
                        </td>
                    </tr>
                    <tr>
                        <th>
                            First Name
                        </th>
                        <td class="fieldMain">
                            <asp:TextBox ID="txtFirstName" runat="server" Width="250px" ToolTip="Enter FirstName" />
                        </td>
                        <td>
                            &nbsp;
                        </td>
                    </tr>
                    <tr>
                        <td colspan="3" class="spacerRowSmall">
                        </td>
                    </tr>
                    <tr runat="server" id="rowInstitution">
                        <th style="text-align: right;">
                            Institution
                        </th>
                        <td class="fieldMain">
                            <asp:DropDownList ID="ddlInstitution" runat="server" Width="255px" AutoPostBack="false"
                                ToolTip="Select Institution" />
                        </td>
                        <td class="fieldOptions">
                            <asp:CheckBox ID="chkInstitution" runat="server" />&nbsp;<span style="color: #666666;">All
                                <b>except</b> the one selected</span>
                        </td>
                    </tr>
                    <tr runat="server" id="rowDepartment">
                        <th>
                            Department
                        </th>
                        <td class="fieldMain">
                            <asp:DropDownList ID="ddlDepartment" runat="server" Width="255px" AutoPostBack="false"
                                ToolTip="Select Department Name" />
                        </td>
                        <td class="fieldOptions">
                            <asp:CheckBox ID="chkDepartment" runat="server" />&nbsp;<span style="color: #666666;">All
                                <b>except</b> the one selected</span>
                        </td>
                    </tr>
                    <tr runat="server" id="rowDivision">
                        <th style="text-align: right;">
                            Division
                        </th>
                        <td class="fieldMain">
                            <asp:DropDownList ID="ddlDivision" runat="server" Width="255px" AutoPostBack="false"
                                ToolTip="Select Division" />
                        </td>
                        <td class="fieldOptions">
                            <asp:CheckBox ID="chkDivision" runat="server" />&nbsp;<span style="color: #666666;">All
                                <b>except</b> the one selected</span>
                        </td>
                    </tr>
                    <tr>
                        <td colspan="3" class="spacerRowSmall">
                        </td>
                    </tr>
                    <tr>
                        <th>
                            Faculty Type
                        </th>
                        <td class="fieldMain">
                            <asp:DropDownList ID="ddlFacultyRank" runat="server" Width="255px" AutoPostBack="false"
                                ToolTip="Select Faculty Type" />
                        </td>
                        <td>
                            &nbsp;
                        </td>
                    </tr>
                    <tr id="trMoreOptions">
                        <th>
                            More Options
                        </th>
                        <td class="fieldMain">
                            <uc1:ComboTreeCheck ID="ctcFirst" runat="server" Width="255px" />
                        </td>
                        <td>
                            &nbsp;
                        </td>
                    </tr>
                </table>
            </div>
            <table class="submitTable">
                <tr>
                    <th>
                        &nbsp;
                    </th>
                    <td>
                        <div style="padding: 12px 0px;">
                            <asp:Button ID="btnSearch" runat="server" Text="Search" OnClick="btnSearch_Click"
                                CssClass="inputButton" />&nbsp;&nbsp;
                            <asp:Button ID="btnSearchReset" runat="server" Text="Clear" OnClick="btnSearchReset_Click"
                                CssClass="inputButton" />
                        </div>
                    </td>
                    <td style="padding-left: 16px;">
                        <asp:LinkButton ID="lnkShowHelpText" runat="server" OnClick="lnkShowHelpText_Click"
                            Visible="false">Explain search options</asp:LinkButton>
                    </td>
                </tr>
                <tr>
                    <th>
                    </th>
                    <td colspan="2">
                        <asp:Literal ID="LtMsg" runat="server" Visible="false" />
                    </td>
                </tr>
            </table>
        </div>
        <asp:PlaceHolder ID="phShowHelpText" runat="server" Visible="false">
            <div style="padding: 0px 50px 12px 50px; color: #444;">
                The institution and department filters each has an "except" option. This allows
                you to select faculty who are not at a particular institution or department. The
                faculty type filter has options to exclude visiting (temporary) and emeritus (retired)
                faculty from the search results or to search only for faculty whose status is full-time.
                Full-time faculty have teaching and/or research and/or clinical scholarship as their
                primary commitment.
            </div>
        </asp:PlaceHolder>
    </asp:Panel>
    <script type="text/javascript" language="javascript">        
        // hack for OpenSocial google search
        var profilesHasSearchResults = false;        
    </script>
    <asp:Panel ID="pnlSearchResults" runat="server" Visible="false">
        <script type="text/javascript" language="javascript">        
            // hack for OpenSocial google search
            profilesHasSearchResults = true;        
        </script>
        <table cellpadding="1" cellspacing="1" border="0" width="600px" style="margin-left: 8px">
            <tr>
                <td valign="middle" align="right">
                    <span class="selectSortLabel">Sort By:</span><asp:ListBox ID="lstSelectSort" runat="server"
                        CssClass="selectSort" OnSelectedIndexChanged="lstSortBy_SelectedIndexChanged"
                        Rows="1" AutoPostBack="true">
                        <asp:ListItem Selected="True" Value="QueryRelevance">Query Relevance</asp:ListItem>
                        <asp:ListItem Selected="False" Value="LastFirstName">Name (A-Z)</asp:ListItem>
                        <asp:ListItem Selected="False" Value="LastFirstName_DESC">Name (Z-A)</asp:ListItem>
                        <asp:ListItem Selected="False" Value="Institution_Fullname">Institution (A-Z)</asp:ListItem>
                        <asp:ListItem Selected="False" Value="Institution_Fullname_DESC">Institution (Z-A)</asp:ListItem>
                        <asp:ListItem Selected="False" Value="Department">Department (A-Z)</asp:ListItem>
                        <asp:ListItem Selected="False" Value="Department_DESC">Department (Z-A)</asp:ListItem>
                        <asp:ListItem Selected="False" Value="FacultyRank">Faculty Rank (low-high)</asp:ListItem>
                        <asp:ListItem Selected="False" Value="FacultyRank_DESC">Faculty Rank (high-low)</asp:ListItem>
                        <%-- <asp:ListItem Selected="False" Value="Division">Division (A-Z)</asp:ListItem>
                        <asp:ListItem Selected="False" Value="Division_DESC">Division (Z-A)</asp:ListItem>--%>
                    </asp:ListBox>
                    <span style="display: none" id="spanDisplayColumns">&nbsp;<span class="selectSortLabel">Display
                        Columns:</span>
                        <asp:ListBox SelectionMode="Multiple" runat="server" ID="lstColumns" Height="20"
                            AutoPostBack="true" OnSelectedIndexChanged="lstColumns_SelectedIndexChanged" />
                    </span>
                </td>
            </tr>
        </table>

        <script language="javascript" type="text/javascript">
            document.getElementById('spanDisplayColumns').style.display = '';   
        </script>

        <div>
            <asp:GridView ID="grdSearchResults" EnableViewState="false" runat="server" AutoGenerateColumns="False"
                AllowPaging="True" AllowSorting="true" CellSpacing="0" GridLines="Both" DataKeyNames="PersonID,Name"
                CssClass="searchResults" PageSize='<%# GetDefaultPageSize()%>' EmptyDataText="<i style='color:#990000'>No matching people could be found</i>"
                DataSourceID="profilesObjDataSource" OnRowDataBound="grdSearchResults_RowDataBound"
                OnDataBound="grdSearchResults_DataBound" OnSorting="grdSearchResults_Sorting"
                OnPageIndexChanged="grdSearchResults_PageIndexChanged" Width="600px">
                <RowStyle CssClass="searchResultsRow" />
                <AlternatingRowStyle CssClass="searchResultsAltRow" />
                <HeaderStyle CssClass="searchResultsHeader" />
                <FooterStyle CssClass="searchResultsFooter" />
                <PagerStyle CssClass="searchResultsPager" />
                <PagerTemplate>
                    <div class="searchResultsPager">
                        <table id="pagerInnerTable" cellpadding="0" cellspacing="0" runat="server" width="100%"
                            border="0">
                            <tr>
                                <td>
                                    <asp:Label EnableViewState="false" ID="lblPageCounter" runat="server" OnLoad="lblPageCounter_Load"
                                        CssClass="pagerText"></asp:Label>
                                </td>
                                <td class="pageGroups">
                                    <span class="pagerText">Page&nbsp;<asp:TextBox EnableViewState="false" ID="txtPageJump"
                                        runat="server" AutoPostBack="true" MaxLength="3" Width="22px" OnDataBinding="txtPageJump_DataBinding"
                                        OnTextChanged="txtPageJump_TextChanged" CssClass="pagerText"></asp:TextBox>&nbsp;of&nbsp;
                                        <asp:Label ID="lblPageCount" runat="server" OnDataBinding="lblPageCount_DataBinding"
                                            CssClass="pagerText"></asp:Label></span>
                                </td>
                                <td>
                                    <span class="pagerText">Per Page&nbsp;</span>
                                    <asp:ListBox EnableViewState="false" ID="lstPageSize" runat="server" Rows="1" OnSelectedIndexChanged="lstPageSize_SelectedIndexChanged"
                                        AutoPostBack="true" OnPreRender="lstPageSize_Load" CssClass="selectSort">
                                        <asp:ListItem Text="15" Value="15"></asp:ListItem>
                                        <asp:ListItem Text="25" Value="25"></asp:ListItem>
                                        <asp:ListItem Text="50" Value="50"></asp:ListItem>
                                        <asp:ListItem Text="100" Value="100"></asp:ListItem>
                                    </asp:ListBox>
                                </td>
                                <td class="pageFirst">
                                    <asp:ImageButton EnableViewState="false" CommandName="Page" Visible='<%# (grdSearchResults.PageIndex > 0) ? true : false %>'
                                        CommandArgument="First" ID="lnkFirstPage" runat="server" ImageUrl="Images/arrow_first.gif">
                                    </asp:ImageButton>
                                </td>
                                <td class="pagePrevNumber">
                                    <asp:LinkButton OnClick="lnkPrevPage_OnClick" EnableViewState="false" CommandName="Page"
                                        Visible='<%# (grdSearchResults.PageIndex > 0) ? true : false %>' CommandArgument="Prev"
                                        ID="lnkPrevPage" runat="server">Previous</asp:LinkButton>
                                </td>
                                <td class="pageNextNumber">
                                    <asp:LinkButton OnClick="lnkNextPage_OnClick" EnableViewState="false" CommandName="Page"
                                        Visible='<%# (grdSearchResults.PageIndex == (grdSearchResults.PageCount - 1)) ? false : true %>'
                                        CommandArgument="Next" ID="lnkNextPage" runat="server">Next</asp:LinkButton>
                                </td>
                                <td class="pageLast">
                                    <asp:ImageButton EnableViewState="false" CommandName="Page" Visible='<%# (grdSearchResults.PageIndex == (grdSearchResults.PageCount - 1)) ? false : true %>'
                                        CommandArgument="Last" ID="lnkLastPage" runat="server" ImageUrl="Images/arrow_last.gif">
                                    </asp:ImageButton>
                                </td>
                                <td>
                                    &nbsp;
                                </td>
                            </tr>
                        </table>
                    </div>
                </PagerTemplate>
                <Columns>
                    <asp:TemplateField HeaderText="Name" SortExpression="LastFirstName">
                        <ItemTemplate>
                            <asp:Label ID="lblName" runat="server" Text='<%# Eval("Name.FullName") %>'></asp:Label>
                            <asp:Image runat="server" ID="Image1" ImageUrl="images/spacer.gif" />
                        </ItemTemplate>
                    </asp:TemplateField>
                    <asp:TemplateField HeaderText="Institution" SortExpression="Institution_Fullname,Institution_Fullname_DESC ">
                        <ItemTemplate>
                            <asp:Label ID="lblInstitution" runat="server" Text='<%# GetInstitutionText(Eval("AffiliationList"))%>' />
                        </ItemTemplate>
                    </asp:TemplateField>
                    <asp:TemplateField HeaderText="Department" SortExpression="Department">
                        <ItemTemplate>
                            <asp:Label ID="lblDepartment" runat="server" Text='<%# GetDepartmentText(Eval("AffiliationList"))%>' />
                        </ItemTemplate>
                    </asp:TemplateField>
                    <asp:TemplateField HeaderText="Division" SortExpression="Division">
                        <ItemTemplate>
                            <asp:Label ID="lblDivision" runat="server" Text='<%# GetDivisionText(Eval("AffiliationList"))%>' />
                        </ItemTemplate>
                    </asp:TemplateField>
                    <asp:TemplateField HeaderText="Faculty Rank" SortExpression="FacultyRank">
                        <ItemTemplate>
                            <asp:Label ID="lblFacultyRank" runat="server" Text='<%# GetFacultyRank(Eval("AffiliationList"))%>' />
                        </ItemTemplate>
                    </asp:TemplateField>
                    <asp:HyperLinkField HeaderText="Why?" Text="Why?" DataNavigateUrlFields="PersonId"
                        DataNavigateUrlFormatString="~/Connection.aspx?Person={0}" Visible="false" />
                    <asp:CommandField ShowSelectButton="True">
                        <HeaderStyle CssClass="hiddenColumn"></HeaderStyle>
                        <ItemStyle CssClass="hiddenColumn"></ItemStyle>
                    </asp:CommandField>
                </Columns>
            </asp:GridView>
        </div>
    </asp:Panel>
    <asp:ObjectDataSource ID="profilesObjDataSource" runat="server" SelectMethod="ProfileSearchPersonBinding"
        SelectCountMethod="GetProfileSearchTotalRowCount" TypeName="Connects.Profiles.Service.ServiceImplementation.ProfileService, Connects.Profiles.Service.ServiceImplementation"
        OnSelecting="profilesObjDataSource_Selecting" OnSelected="profilesObjDataSource_OnSelected"
        EnableCaching="true" CacheDuration="120" CacheKeyDependency="profilesObjDataSourceKey"
        EnablePaging="true" MaximumRowsParameterName="maximumRows" EnableViewState="true">
    </asp:ObjectDataSource>

<%-- Profiles OpenSocial Extension by UCSF --%>    
    <asp:Panel ID="pnlOpenSocialGadgets" runat="server" Visible="false">
        <script type="text/javascript" language="javascript">
            // find the 'Search' gadget(s).  
            var searchGadgets = my.findGadgetsAttachingTo("gadgets-search");
            var keyword = '<%=GetKeyword()%>';
            // add params to these gadgets
            if (keyword && <%=HasAdditionalSearchCriteria() ? 0 : 1 %>) {
                for (var i = 0; i < searchGadgets.length; i++) {
                    var searchGadget = searchGadgets[i];
                    searchGadget.additionalParams = searchGadget.additionalParams || {};
                    searchGadget.additionalParams["keyword"] = keyword;
                    // hack to open if no results are returned
                    searchGadget.start_closed = profilesHasSearchResults;
                }
            }
            else {  // remove these gadgets
                my.removeGadgets(searchGadgets);
            }            
        </script>
        <div id="gadgets-search" class="gadgets-gadget-parent"></div>
    </asp:Panel>

</asp:Content>

<asp:Content ID="Content2" ContentPlaceHolderID="RightContentPlaceHolder" runat="Server">
    <div id="divSpotlight" runat="server" class="passive_container">
        <!-- Passive Networks Start -->
        <div class="passive_section_head">
            Most viewed today</div>
        <!-- Passive Networks End -->
        <div class="passive_section_body">
            <ul>
                <asp:Repeater ID="rptMostViewedToday" runat="server">
                    <ItemTemplate>
                        <li>
                            <asp:HyperLink ID="hypLnkMostViewedToday" runat="server" Text='<%#Eval("keyword") %>'
                                NavigateUrl='<%# "~\\Search.aspx?From=MeSH&Word=" + Eval("keyword") %>' /></li>
                    </ItemTemplate>
                </asp:Repeater>
            </ul>
        </div>
        <div class="passive_section_line" visible="true">
        </div>
        <!-- Passive Networks Start -->
        <div class="passive_section_head">
            Most viewed this Month</div>
        <!-- Passive Networks End -->
        <div class="passive_section_body">
            <ul>
                <asp:Repeater ID="rptMostViewedMonth" runat="server">
                    <ItemTemplate>
                        <li>
                            <asp:HyperLink ID="hypLnkMostViewedToday" runat="server" Text='<%#Eval("keyword") %>'
                                NavigateUrl='<%# "~\\Search.aspx?From=MeSH&Word=" + Eval("keyword") %>'></asp:HyperLink></li>
                    </ItemTemplate>
                </asp:Repeater>
            </ul>
        </div>
    </div>
    <div id="divSearchCriteria" runat="server">
        <div class="passive_section_body">
            <asp:Repeater ID="lstSearchCriteriaDisplay" runat="server">
                <HeaderTemplate>
                    <asp:Label ID="lblSearchCriteria" runat="server" Text="Search Criteria" CssClass="menuWidgetTitleRight"></asp:Label>
                    <ul>
                </HeaderTemplate>
                <ItemTemplate>
                    <li>
                        <asp:Label ID="lblCriteria" runat="server" Text='<%#Container.DataItem%>'></asp:Label>
                    </li>
                </ItemTemplate>
                <FooterTemplate>
                    </ul>
                    <br />
                </FooterTemplate>
            </asp:Repeater>
        </div>
        <div class="passive_section_body">
            <asp:Repeater ID="lstSearchKeywordDisplay" OnItemDataBound="lstSearchKeywordDisplay_ItemDataBound" runat="server">
                <HeaderTemplate>
                    <div class="menuWidgetTitleRight">
                        <b>Keywords</b></div>
                    <ul>
                </HeaderTemplate>
                <ItemTemplate>
                    <li>
                        <%# Eval("Keyword") %>
                    </li>
                </ItemTemplate>
                <FooterTemplate>
                    </ul>
                    
                    <asp:Panel id="divDirect" runat="server" Visible="false">                                            
                        </p>
                        </hr>
                        <a href="direct.aspx?SearchPhrase=<%=GetKeyword()%>">Search other institutions</a>
                    </asp:Panel>
                </FooterTemplate>
            </asp:Repeater>
            
        </div>
    </div>
    <div id="searchResultsPersonSummaryContainer" runat="server">
        <div id="searchResultsPersonSummary">
            <div class="passive_section_line">
            </div>
            <div id="divPersonSummary" class="passive_section_head">
                Person Summary</div>
            <div class="passive_section_body" id="searchResultsPersonSummaryContent">
            </div>
        </div>
    </div>
</asp:Content>
