<%@ Page Title="" Language="C#" AutoEventWireup="true" 
MasterPageFile="~/ProfilesPage.master" 
CodeFile="gadgetlogin.aspx.cs" Inherits="gadgetlogin" %>

<asp:Content ID="Content1" ContentPlaceHolderID="MiddleContentPlaceHolder" runat="Server">
  <div id="contentPosition">
    <div class="pageTitle"><asp:Literal ID="ltHeader" runat="server" /></div>
    <div style="margin-top: 10px;">
        <asp:Panel ID="pnlProxySearch" runat="server" DefaultButton="btnGadgetLogin">
            <table border="0" cellspacing="0" cellpadding="0" class="mainSearchForm">
                <tr>
                    <th>
                        Person Id
                    </th>
                    <td>
                        <asp:TextBox ID="txtPersonId" runat="server" Width="250px" ToolTip="Enter Person ID" />
                    </td>
                </tr>
                <tr>
                    <th>
                        Password
                    </th>
                    <td>
                        <asp:TextBox ID="txtPassword" runat="server" TextMode="Password" Width="250px" ToolTip="Enter Password" />
                    </td>
                </tr>
                <tr>
                    <th>
                        Gadgets</p>
                        One Per Line
                    </th>
                    <td>
                        <asp:TextBox ID="txtGadgetURLS" runat="server" rows="5" TextMode="multiline" Width="400px" ToolTip="Enter Gadget URLs" />
                    </td>
                </tr>
                <tr>
                    <td>
                        <asp:CheckBox ID="chkDebug" runat="server" Text="Debug Mode" Checked="false" />
                    </td>
                </tr>
                <tr>
                    <td>
                        <asp:CheckBox ID="chkUseCache" runat="server" Text="Use Cache" Checked="true" />
                    </td>
                </tr>
                <tr>
                    <th>
                    </th>
                    <td>
                        <div style="padding: 12px 0px;">
                            <asp:Button ID="btnGadgetLogin" runat="server" Text="Login" OnClick="btnGadgetLogin_Click" />
                        </div>
                    </td>
                </tr>
            </table>
            <div class="sectionHeader"><asp:Literal ID="litGridHeader" runat="server" /></div>
        </asp:Panel>
    </div>
  </div>
</asp:Content>