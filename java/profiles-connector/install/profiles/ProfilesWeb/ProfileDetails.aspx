<%@ Page Language="C#" MasterPageFile="~/ProfilesPage.master" AutoEventWireup="true"
    CodeFile="ProfileDetails.aspx.cs" Inherits="ProfileDetails" EnableEventValidation="false"
    ValidateRequest="false" %>

<%@ Register Src="UserControls/ucProfileRightSide.ascx" TagName="ProfileRightSide"
    TagPrefix="ucProfile" %>
<%@ Register Src="UserControls/ucModifyNetwork.ascx" TagName="ModifyNetwork" TagPrefix="ucModifyNetwork" %>
<%@ Register Src="UserControls/ucProfileBaseInfo.ascx" TagName="ProfileBaseInfo"
    TagPrefix="ucProfileBaseInfo" %>
     <%--
    Copyright (c) 2008-2010 by the President and Fellows of Harvard College. All rights reserved.  
    Profiles Research Networking Software was developed under the supervision of Griffin M Weber, MD, PhD.,
    and Harvard Catalyst: The Harvard Clinical and Translational Science Center, with support from the 
    National Center for Research Resources and Harvard University.


    Code licensed under a BSD License. 
    For details, see: LICENSE.txt 
 --%> 
    
<asp:Content ID="Content3" ContentPlaceHolderID="LeftContentPlaceHolder" runat="Server">
    <ucModifyNetwork:ModifyNetwork ID="ucModifyNetwork" runat="server" />
</asp:Content>
<asp:Content ID="Content1" ContentPlaceHolderID="MiddleContentPlaceHolder" runat="Server">

    <script src="Scripts/JScript.js" type="text/jscript"></script>

    <table width="100%">
        <tr>
            <td>
                <div class="pageTitle">
                    <asp:Literal ID="ltProfileName" runat="server" /><asp:Image ID="imgReach" runat="server"
                        BorderStyle="None" Visible="false" CssClass="reachImage" /></div>
            </td>
            <td align="right" valign="top">
                <div class="pageBackLink">
                    <asp:HyperLink ID="hypBack" runat="server" Style="cursor: pointer;" Visible="false">Back</asp:HyperLink>
                    <asp:HiddenField ID="hdnBack" runat="server" />
                </div>
            </td>
        </tr>
    </table>
    <asp:Panel ID="pnlReadOnlySection" runat="server" Visible="true">
        <table width="100%" cellpadding="1" cellspacing="1">
            <tr>
                <td>
                    <ucProfileBaseInfo:ProfileBaseInfo ID="ucProfileBaseInfo" runat="server" Visible="true" EditMode="false"/>
                </td>
                <td style="vertical-align: top; width: 120px">
                    <asp:Image ID="imgReadPhoto" runat="server" Height="120px" Width="120px" Style="border: solid 1px #999;"
                        ImageUrl="images/photobigconf.jpg" />
                </td>
            </tr>
        </table>
        <div style="clear: both">
        </div>
        <div id="pnlReadoOnlyAwardsHonors" runat="server" visible="true">
            <div class="sectionHeader">
                <asp:Literal ID="ltrReadAwardsHonors" runat="server" Text="Awards and Honors" Visible="true" />
            </div>
            <div>
                <asp:Repeater ID="RptrReadAwardsHonors" runat="server">
                    <HeaderTemplate>
                        <table cellpadding="2" cellspacing="2" border="0">
                    </HeaderTemplate>
                    <ItemTemplate>
                        <tr>
                            <td>
                                <asp:Label ID="lblAwardStartYear" runat="server" Text='<%#Eval("AwardStartYear") %>' />
                            </td>
                            <td>
                                <asp:Label ID="lblAwardName" runat="server" Text='<%# String.Format("{0} {1}", Eval("AwardInstitution_Fullname"), Eval("AwardName")) %>' />
                            </td>
                        </tr>
                    </ItemTemplate>
                    <FooterTemplate>
                        </table></FooterTemplate>
                </asp:Repeater>
            </div>
        </div>
        <div id="pnlReadOnlyNarrative" runat="server">
            <div class="sectionHeader">
                <asp:Literal ID="ltrReadNarrative" runat="server" Text="Narrative" Visible="true" />
            </div>
            <div>
                <asp:Label ID="lblReadOnlyNarrative" runat="server" CssClass="narrativeText" />
            </div>
        </div>
        
        <%-- Profiles OpenSocial Extension by UCSF --%>    
        <div id="pnlOpenSocialGadgets" runat="server" style="margin-top:18px">
            <script type="text/javascript" language="javascript">
                my.current_view = "profile";
            </script>                
            <div id="gadgets-view" class="gadgets-gadget-parent"></div>
        </div>
        <div style="clear: both">
        </div>
        
        <div id="pnlReadOnlyPublications" runat="server">
            <div class="sectionHeader">
                <asp:Literal ID="ltrReadPublications" runat="server" Text="Publications" Visible="true" />
            </div>
            <div>
                <asp:Repeater ID="rptPublications" runat="server">
                    <HeaderTemplate>
                        <table cellpadding="0" cellspacing="0" border="0">
                    </HeaderTemplate>
                    <ItemTemplate>
                        <tr>
                            <td valign="top" align="left">
                                <asp:Label ID="lblPubIndex" runat="server" Text='<%# String.Format("{0}.", Container.ItemIndex + 1) %>'
                                    CssClass="publicationRowText" />
                            </td>
                            <td>
                                <asp:Label ID="lblPubRef" runat="server" Text='<%# Eval("PublicationReference") %>'
                                    CssClass="publicationRowText" />
                            </td>
                        </tr>
                        <div id="divPublicationSource" runat="server">
                            <tr>
                                <td>
                                    &nbsp;
                                </td>
                                <td>
                                    <asp:Repeater ID="rptPubSource" runat="server" DataSource='<%# Eval("PublicationSourceList") %>'
                                        OnItemDataBound="rptPubSource_ItemDataBound">
                                        <HeaderTemplate>
                                            <span class="viewInLabel">View in:</span>
                                        </HeaderTemplate>
                                        <ItemTemplate>
                                            <asp:HyperLink ID="lnkPubLoc" runat="server" Text='<%# Eval("Name") %>' NavigateUrl='<%# Eval("URL") %>'
                                                Target="_new" OnDataBinding="lnkPubLoc_DataBinding"></asp:HyperLink>
                                        </ItemTemplate>
                                    </asp:Repeater>
                                </td>
                            </tr>
                        </div>
                        <tr>
                            <td>
                                &nbsp;
                            </td>
                            <td>
                                <div class="LineSeperatorPubs" style="width: 100%">
                                </div>
                            </td>
                        </tr>
                    </ItemTemplate>
                    <FooterTemplate>
                        </table></FooterTemplate>
                </asp:Repeater>
            </div>
        </div>
    </asp:Panel>
    <div class="subSectionHeader">
        <asp:Literal ID="txtProfileProblem" runat="server"></asp:Literal></div>
    <%--    <asp:Panel ID="pnlControl" runat="server" Visible="false">
        <asp:PlaceHolder ID="plToControl" runat="server" />
    </asp:Panel>--%>
    <%--    <asp:HiddenField ID="hdnValue" runat="server" Visible="true" />--%>
</asp:Content>
<asp:Content ID="Content2" ContentPlaceHolderID="RightContentPlaceHolder" runat="Server">
    <asp:UpdatePanel ID="upnlRightCol" runat="server" UpdateMode="Conditional">
        <ContentTemplate>
            <div class="rightColumnWidget">
                <ucProfile:ProfileRightSide ID="ProfileRightSide1" runat="server" />
            </div>
        </ContentTemplate>
    </asp:UpdatePanel>
</asp:Content>
