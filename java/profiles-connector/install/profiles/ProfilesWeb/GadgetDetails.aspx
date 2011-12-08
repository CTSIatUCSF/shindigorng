<%@ Page Language="C#" MasterPageFile="~/ProfilesPage.master" AutoEventWireup="true"
    CodeFile="GadgetDetails.aspx.cs" Inherits="GadgetDetails" EnableEventValidation="false" Title="UCSF Profiles"
    ValidateRequest="false" %>

<asp:Content ID="Content1" ContentPlaceHolderID="MiddleContentPlaceHolder" runat="Server">

    <table width="100%">
        <tr>
            <td>
                <div class="pageTitle" id="gadgets-title">Results</div><asp:Image ID="imgReach" runat="server"
                        BorderStyle="None" Visible="false" CssClass="reachImage" />
            </td>
            <td align="right" valign="top">
                <div class="pageBackLink">
                    <asp:HyperLink ID="hypBack" runat="server" Style="cursor: pointer;" Visible="true">Back</asp:HyperLink>
                </div>
            </td>
        </tr>
    </table>
    <%-- Profiles OpenSocial Extension by UCSF --%>    
    <asp:Panel ID="pnlOpenSocialGadgets" runat="server" Visible="true">
        <script type="text/javascript" language="javascript">
            my.current_view = "profile";
        </script>                
        <div id="gadgets-detail" class="gadgets-gadget-parent"></div>
        
    </asp:Panel>

</asp:Content>
<asp:Content ID="Content2" ContentPlaceHolderID="RightContentPlaceHolder" runat="Server">
&nbsp;
</asp:Content>
