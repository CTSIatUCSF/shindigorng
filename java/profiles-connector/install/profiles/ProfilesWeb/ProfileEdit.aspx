<%@ Page Language="C#" MasterPageFile="~/ProfilesPage.master" AutoEventWireup="true"
    CodeFile="ProfileEdit.aspx.cs" Inherits="ProfileEdit" EnableEventValidation="false"
    ValidateRequest="false" %>

<%@ Register Src="UserControls/ucProfileRightSide.ascx" TagName="ProfileRightSide"
    TagPrefix="ucProfile" %>
<%@ Register Src="UserControls/ucProfileBaseInfo.ascx" TagName="ProfileBaseInfo"
    TagPrefix="ucProfileBaseInfo" %>
<%@ Register Assembly="FUA" Namespace="Subgurim.Controles" TagPrefix="cc1" %>
 <%--
    Copyright (c) 2008-2010 by the President and Fellows of Harvard College. All rights reserved.  
    Profiles Research Networking Software was developed under the supervision of Griffin M Weber, MD, PhD.,
    and Harvard Catalyst: The Harvard Clinical and Translational Science Center, with support from the 
    National Center for Research Resources and Harvard University.


    Code licensed under a BSD License. 
    For details, see: LICENSE.txt 
 --%> 
<asp:Content ID="Content1" ContentPlaceHolderID="MiddleContentPlaceHolder" runat="Server">

    <script src="Scripts/JScript.js" type="text/jscript"></script>

    <script language="javascript" type="text/javascript">
        function checkFileExtension(elem) {
            var filePath = elem.value;

            if (filePath.indexOf('.') == -1)
                return false;

            var validExtensions = new Array();
            var ext = filePath.substring(filePath.lastIndexOf('.') + 1).toLowerCase();

            validExtensions[0] = 'jpg';
            validExtensions[1] = 'gif';
            validExtensions[2] = 'bmp';
            validExtensions[3] = 'png';

            for (var i = 0; i < validExtensions.length; i++) {
                if (ext == validExtensions[i])
                    return true;
            }

            alert('The file extension ' + ext.toUpperCase() + ' is not allowed.');

            return false;
        }

        function exclusiveCheckbox(rbNum) {

            var rbNamePre = "ctl00_ctl00_middle_MiddleContentPlaceHolder_dlPhotos_ctl0";

            var test = document.getElementById(rbNamePre);

            if (test == null) {                
                rbNamePre = "middle_MiddleContentPlaceHolder_dlPhotos_ctl0";
                test = document.getElementById(rbNamePre);
            }
            if (test == null) { return; }



            var rbNamePost = "_rbPhoto";

            var i = 0;
            var rb = null;
            for (i = 0; i <= 8; i++) {

                if (i != rbNum) {
                    rb = document.getElementById(rbNamePre + i + rbNamePost);

                    if (rb != null)
                        rb.checked = false;
                }
            }

            var customMode = false;
            if (rbNum == 9)
                customMode = true;

            if (document.getElementById("ctl00_ctl00_middle_MiddleContentPlaceHolder_rbPhotoCustom") != null) {
                document.getElementById("ctl00_ctl00_middle_MiddleContentPlaceHolder_rbPhotoCustom").checked = customMode;
            }
            else { document.getElementById("middle_MiddleContentPlaceHolder_rbPhotoCustom").checked = customMode; }


            if (document.getElementById("ctl00_ctl00_middle_MiddleContentPlaceHolder_hidRbTrack") != null) {
                document.getElementById("ctl00_ctl00_middle_MiddleContentPlaceHolder_hidRbTrack").value = rbNum;
            } else { document.getElementById("middle_MiddleContentPlaceHolder_hidRbTrack").value = rbNum; }


            return true;
        }
    </script>

    <table class="EditBody">
        <tr>
            <td style="padding-right: 10px;">
                <div style="width: 70%; float: left;">
                    <h2>
                        <asp:Literal ID="ltProfileName" runat="server" /></h2>
                </div>
                <div style="text-align: right;">
                    <asp:PlaceHolder ID="phEditModeLabel" runat="server" Visible="false">
                        <asp:Label ID="lblEditMode" runat="server" Text="EDIT MODE" Style="font-weight: bold;
                            color: #cc0000;"></asp:Label>
                        &nbsp;|&nbsp;<asp:HyperLink ID="hypLnkReturn" runat="server" Text="View profile"
                            CssClass="hypLinks" />
                    </asp:PlaceHolder>
                    <asp:HyperLink ID="hypLnkViewProfile" runat="server" Text="Back to profile" CssClass="hypLinks"
                        Visible="false" />
                </div>
                <div style="clear: left;">
                </div>
                <asp:UpdatePanel ID="upnlEditSection" runat="server" UpdateMode="Conditional">
                    <Triggers>
                        <asp:AsyncPostBackTrigger ControlID="btnEditPhoto" />
                        <asp:AsyncPostBackTrigger ControlID="btnClose" />
                    </Triggers>
                    <ContentTemplate>

                        <script language="javascript" type="text/javascript">
                            function setImage() {
                                var pageURL = parent.document.URL;
                                var personId = pageURL.substring(pageURL.indexOf('Person') + 7, pageURL.length)
                                var randomnumber = Math.floor(Math.random() * 999)

                                img = document.getElementById('ctl00_ctl00_middle_MiddleContentPlaceHolder_imgEditPhoto3');
                                if (img == null) {
                                    img = document.getElementById('middle_MiddleContentPlaceHolder_imgEditPhoto3');
                                    if (img == null) { return; }
                                }
                                try {
                                    img.src = "Thumbnail.ashx?id=" + personId + "&rnd=" + randomnumber;
                                } catch (err) { }

                                var lnk = window.parent.document.getElementById('ctl00_ctl00_middle_MiddleContentPlaceHolder_lnkAddCustomPhoto')

                                if (lnk == null) {

                                    lnk = window.parent.document.getElementById('middle_MiddleContentPlaceHolder_lnkAddCustomPhoto')
                                    if (lnk == null) { return; }

                                }
                                lnk.style.display = 'block';

                                return;
                            }
                        </script>

                        <table width="100%" border="0">
                            <tr>
                                <td>
                                    <ucProfileBaseInfo:ProfileBaseInfo ID="ucProfileBaseInfo" runat="server" Visible="true" EditMode="true"/>
                                </td>
                                <td style="vertical-align: top; text-align: right;">
                                    <asp:Panel ID="pnlEditPhoto" runat="server">
                                        <table border="0">
                                            <tr>
                                                <td style="vertical-align: top;">
                                                    <asp:Image ID="imgEditPhoto" runat="server" Height="120px" Width="120px" Style="border: solid 1px #999;" />
                                                </td>
                                            </tr>
                                            <tr>
                                                <td align="center">
                                                    <asp:LinkButton ID="btnHidePhoto" runat="server" OnClick="btnHidePhoto_OnClick">Hide</asp:LinkButton>
                                                    <asp:Label ID="lblHidePhoto" runat="server" Text="Hide" CssClass="NormalText"></asp:Label>
                                                    <b>&nbsp;|&nbsp;</b>
                                                    <asp:LinkButton ID="btnShowPhoto" runat="server" OnClick="btnShowPhoto_OnClick" OnClientClick="JavaScript:return confirm('Are you sure you want to make this photo visible to the public?')">Show</asp:LinkButton>
                                                    <asp:Label ID="lblShowPhoto" runat="server" Text="Show" CssClass="NormalText"></asp:Label>
                                                    <b>&nbsp;|&nbsp;</b>
                                                    <asp:LinkButton ID="btnEditPhoto" runat="server" OnClientClick="setImage();" Text="Edit"></asp:LinkButton>
                                                    <br />
                                                    <asp:Label ID="lblVisiblePhoto" runat="server" Text="Photo is VISIBLE" ForeColor="#4faa4f"
                                                        Font-Bold="true"></asp:Label>
                                                    <asp:Label ID="lblHiddenPhoto" runat="server" Text="Photo is HIDDEN" ForeColor="#cc0000"
                                                        Font-Bold="true"></asp:Label>
                                                </td>
                                            </tr>
                                        </table>
                                    </asp:Panel>
                                </td>
                            </tr>
                        </table>
                        <div id="pnlPhotoPopup" class="photoEditPopup" runat="server" style="display: block;">
                            <div>
                                <div class="photoEditTitle">
                                    Change Photo</div>
                                <div class="photoEditSubTitle">
                                    Select from a custom photograph or image.</div>
                            </div>
                            <div>
                                <table>
                                    <tr>
                                        <td>
                                            <asp:RadioButton ID="rbPhotoCustom" runat="server" Text="Custom Photo" OnLoad="rbPhotoCustom_Load" />
                                        </td>
                                        <td rowspan="2">
                                            <asp:DataList runat="server" ID="dlPhotos" RepeatDirection="Horizontal" RepeatLayout="Table"
                                                OnLoad="dlPhotos_OnLoad">
                                                <ItemTemplate>
                                                    <asp:RadioButton runat="server" ID="rbPhoto" OnDataBinding="rbPhoto_DataBinding"
                                                        OnCheckedChanged="rbPhoto_CheckedChanged" />
                                                    <br />
                                                    <asp:Image ID="imgPhoto" runat="server" Height="120px" Width="120px" Style="border: solid 1px #999;"
                                                        ImageUrl='<%#Eval("PhotoLink") %>' OnDataBinding="imgPhoto_DataBinding" CausesValidation="false" />
                                                </ItemTemplate>
                                            </asp:DataList>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td>
                                            <asp:Image ID="imgEditPhoto3" runat="server" Height="120px" Width="120px" Style="border: solid 1px #999;" />
                                        </td>
                                    </tr>
                                    <tr>
                                        <td>
                                            <asp:LinkButton runat="server" ID="lnkAddCustomPhoto" Text="Add Custom Photo" OnClientClick="var myBox=document.getElementById('divPhotoUpload');myBox.style.display=(myBox.style.display=='none' ? 'block': 'none');return false;"></asp:LinkButton>
                                        </td>
                                        <td>
                                            <asp:HiddenField ID="hidRbTrack" runat="server" />
                                        </td>
                                    </tr>
                                </table>
                            </div>
                            <div id="divPhotoUpload" style="display: none;">
                                <div class="photoEditSubTitle">
                                    You can upload a JPG, GIF, BMP, or PNG file. Maximum file size is 256K.</div>
                                <cc1:FileUploaderAJAX ID="FileUpload1" runat="server" Width="300" MaxFiles="1" text_X=""
                                    text_Add="Upload Photo" text_Delete="" showDeletedFilesOnPostBack="false" text_Uploading="Uploading" />
                                <br />
                            </div>
                            <div>
                                <asp:Label ID="lblFileUploadError" runat="server"></asp:Label></div>
                            <div class="photoEditButtonDiv">
                                <asp:LinkButton runat="server" ID="btnSaveClose" Text="Save" OnClick="btnSaveClose_Click"
                                    CssClass="photoEditButton" />
                                <asp:LinkButton runat="server" ID="btnClose" Text="Close" OnClick="btnClose_Click"
                                    CssClass="photoEditButton" />
                            </div>
                        </div>
                        <ajaxtoolkit:ModalPopupExtender ID="MPE" runat="server" TargetControlID="btnEditPhoto"
                            PopupControlID="pnlPhotoPopup" BackgroundCssClass="modalBackground" DropShadow="false" />
                        <div id="ProfileDetails">
                            <table width="100%">
                                <tr style="height: 4px">
                                    <td>
                                        <asp:Literal ID="txtProfileProblem" runat="server"></asp:Literal>
                                    </td>
                                </tr>
                                <tr style="height: 6px">
                                    <td>
                                    </td>
                                </tr>
                                <tr>
                                    <td>
                                        <asp:Panel ID="pnlEditAwardsHonors" runat="server" CssClass="Borders">
                                            <table id="tblEditAwardsHonors" width="100%">
                                                <tr>
                                                    <td style="text-align: left; white-space: nowrap;">
                                                        <h3>
                                                            Awards and Honors</h3>
                                                    </td>
                                                    <td style="text-align: center; white-space: nowrap;">
                                                        <asp:Label ID="lblVisibleAward" runat="server" Text="This section is VISIBLE to the public"
                                                            ForeColor="#4faa4f" Font-Bold="true"></asp:Label>
                                                        <asp:Label ID="lblHiddenAward" runat="server" Text="This section is HIDDEN from the public"
                                                            ForeColor="#cc0000" Font-Bold="true"></asp:Label>
                                                    </td>
                                                    <td style="text-align: right; white-space: nowrap;">
                                                        <asp:LinkButton ID="btnHideAwards" runat="server" OnClick="btnHideAwards_OnClick">Hide</asp:LinkButton>
                                                        <asp:Label runat="server" ID="lblHideAwards" Text="Hide" CssClass="NormalText"></asp:Label>
                                                        <b>&nbsp;|&nbsp;</b>
                                                        <asp:LinkButton ID="btnShowAwards" runat="server" OnClick="btnShowAwards_OnClick"
                                                            OnClientClick="JavaScript:return confirm('Are you sure you want to make this section visible to the public?')">Show</asp:LinkButton>
                                                        <asp:Label ID="lblShowAwards" runat="server" Text="Show" CssClass="NormalText"></asp:Label>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td colspan="3">
                                                        <div style="padding: 10px 0px;">
                                                            <asp:ImageButton ID="btnImgEditAwards" runat="server" ImageUrl="~/Images/icon_squareArrow.gif"
                                                                OnClick="btnEditAwards_OnClick" />&nbsp;
                                                            <asp:LinkButton ID="btnEditAwards" runat="server" OnClick="btnEditAwards_OnClick"
                                                                CssClass="profileHypLinks">Add award(s)</asp:LinkButton>
                                                        </div>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td colspan="3">
                                                        <asp:Label ID="lblNoAwards" runat="server" Text="None" Font-Italic="true" ForeColor="#999"
                                                            Visible="false" />
                                                        <asp:Repeater ID="RptrEditAwards" runat="server" Visible="false">
                                                            <ItemTemplate>
                                                                <asp:Label ID="lblEditAwards" runat="server" Text='<%#Eval("AwardsHonors").ToString() %>' />
                                                                <br />
                                                            </ItemTemplate>
                                                        </asp:Repeater>
                                                        <asp:Panel ID="pnlInsertAward" runat="server" Style="background-color: #F0F4F6; margin-bottom: 5px;
                                                            border: solid 1px #999;" Visible="false" DefaultButton="btnInsertAward">
                                                            <table border="0" cellspacing="2" cellpadding="4">
                                                                <tr>
                                                                    <td colspan="3">
                                                                        <div style="padding-top: 5px;">
                                                                            Enter the year(s), name and institution.
                                                                        </div>
                                                                        <div style="padding-top: 3px;">
                                                                            For Award Year(s), enter both fields only if awarded for consecutive years.
                                                                        </div>
                                                                    </td>
                                                                </tr>
                                                                <tr>
                                                                    <td>
                                                                        <b>Award Year(s)</b><br />
                                                                        <asp:TextBox ID="txtFoot1" runat="server" MaxLength="4" Width="30px" TabIndex="1"></asp:TextBox>
                                                                        &nbsp;<b>-</b>&nbsp;
                                                                        <asp:TextBox ID="txtFoot2" runat="server" MaxLength="4" Width="30px" TabIndex="2"></asp:TextBox>
                                                                    </td>
                                                                    <td>
                                                                        <b>Name</b><br />
                                                                        <asp:TextBox ID="txtFoot3" runat="server" MaxLength="100" TabIndex="3" Width="220px"></asp:TextBox>
                                                                    </td>
                                                                    <td>
                                                                        <b>Institution</b><br />
                                                                        <asp:TextBox ID="txtFoot4" runat="server" MaxLength="100" TabIndex="4" Width="220px"></asp:TextBox>
                                                                    </td>
                                                                </tr>
                                                                <tr>
                                                                    <td colspan="3">
                                                                        <div style="padding-bottom: 5px; text-align: left;">
                                                                            <asp:LinkButton ID="btnInsertAward" runat="server" CausesValidation="False" OnClick="btnInsert_OnClick"
                                                                                Text="Save and add another" TabIndex="5"></asp:LinkButton>
                                                                            &nbsp;&nbsp;<b>|</b>&nbsp;&nbsp;
                                                                            <asp:LinkButton ID="btnInsertAward2" runat="server" CausesValidation="False" OnClick="btnInsertClose_OnClick"
                                                                                Text="Save and Close" TabIndex="6"></asp:LinkButton>
                                                                            &nbsp;&nbsp;<b>|</b>&nbsp;&nbsp;
                                                                            <asp:LinkButton ID="btnInsertCancel" runat="server" CausesValidation="False" OnClick="btnInsertCancel_OnClick"
                                                                                Text="Close" TabIndex="7"></asp:LinkButton>
                                                                        </div>
                                                                    </td>
                                                                </tr>
                                                            </table>
                                                        </asp:Panel>
                                                        <asp:GridView ID="GridViewAwards" runat="server" AutoGenerateColumns="False" CellPadding="4"
                                                            DataKeyNames="awardid" DataSourceID="AwardsDS" EmptyDataText="&lt;div style=&quot;font-style:italic; color:#999; &quot;&gt;None&lt;/div&gt;"
                                                            GridLines="Horizontal" OnRowCancelingEdit="GridViewAwards_RowCancelingEdit" OnRowDataBound="GridViewAwards_RowDataBound"
                                                            OnRowDeleted="GridViewAwards_RowDeleted" OnRowEditing="GridViewAwards_RowEditing"
                                                            OnRowUpdated="GridViewAwards_RowUpdated" OnRowUpdating="GridViewAwards_RowUpdating"
                                                            Width="100%">
                                                            <Columns>
                                                                <asp:TemplateField HeaderText="Year&nbsp;of Award">
                                                                    <EditItemTemplate>
                                                                        <asp:TextBox ID="txtYr1" runat="server" MaxLength="4" Text='<%# Bind("yr") %>'></asp:TextBox>
                                                                    </EditItemTemplate>
                                                                    <ItemTemplate>
                                                                        <asp:Label ID="Label1" runat="server" Text='<%# Bind("yr") %>'></asp:Label>
                                                                    </ItemTemplate>
                                                                    <ControlStyle Width="35px" />
                                                                    <HeaderStyle HorizontalAlign="Center" />
                                                                    <ItemStyle HorizontalAlign="Center" />
                                                                </asp:TemplateField>
                                                                <asp:TemplateField HeaderText="Thru Year">
                                                                    <EditItemTemplate>
                                                                        <asp:TextBox ID="txtYr2" runat="server" MaxLength="4" Text='<%# Bind("yr2") %>'></asp:TextBox>
                                                                    </EditItemTemplate>
                                                                    <ItemTemplate>
                                                                        <asp:Label ID="Label2" runat="server" Text='<%# Bind("yr2") %>'></asp:Label>
                                                                    </ItemTemplate>
                                                                    <ControlStyle Width="35px" />
                                                                    <HeaderStyle HorizontalAlign="Center" />
                                                                    <ItemStyle HorizontalAlign="Center" />
                                                                </asp:TemplateField>
                                                                <asp:TemplateField HeaderText="Name">
                                                                    <EditItemTemplate>
                                                                        <asp:TextBox ID="txtAwardName" runat="server" MaxLength="100" Text='<%# Bind("awardnm") %>'></asp:TextBox>
                                                                    </EditItemTemplate>
                                                                    <ItemTemplate>
                                                                        <asp:Label ID="Label3" runat="server" Text='<%# Bind("awardnm") %>'></asp:Label>
                                                                    </ItemTemplate>
                                                                    <ItemStyle Wrap="true" />
                                                                </asp:TemplateField>
                                                                <asp:TemplateField HeaderText="Institution">
                                                                    <EditItemTemplate>
                                                                        <asp:TextBox ID="txtAwardInst" runat="server" MaxLength="100" Text='<%# Bind("awardinginst") %>'></asp:TextBox>
                                                                    </EditItemTemplate>
                                                                    <ItemTemplate>
                                                                        <asp:Label ID="Label4" runat="server" Text='<%# Bind("awardinginst") %>'></asp:Label>
                                                                    </ItemTemplate>
                                                                    <ItemStyle Wrap="true" />
                                                                </asp:TemplateField>
                                                                <asp:TemplateField ShowHeader="False">
                                                                    <EditItemTemplate>
                                                                        <asp:LinkButton ID="LinkButton1" runat="server" CausesValidation="True" CommandName="Update"
                                                                            Text="Update"></asp:LinkButton>
                                                                        <asp:LinkButton ID="LinkButton2" runat="server" CausesValidation="False" CommandName="Cancel"
                                                                            Text="Cancel"></asp:LinkButton>
                                                                    </EditItemTemplate>
                                                                    <ItemTemplate>
                                                                        <asp:LinkButton ID="LinkButton3" runat="server" CausesValidation="False" CommandName="Edit"
                                                                            Text="Edit"></asp:LinkButton>
                                                                    </ItemTemplate>
                                                                    <ItemStyle HorizontalAlign="Center" Width="25px" />
                                                                </asp:TemplateField>
                                                                <asp:TemplateField ShowHeader="False">
                                                                    <ItemTemplate>
                                                                        <asp:LinkButton ID="LinkButton4" runat="server" CausesValidation="False" CommandName="Delete"
                                                                            OnClientClick="Javascript:return confirm('Are you sure you want to delete this entry?');"
                                                                            Text="X"></asp:LinkButton>
                                                                    </ItemTemplate>
                                                                    <ItemStyle HorizontalAlign="Center" Width="10px" />
                                                                </asp:TemplateField>
                                                            </Columns>
                                                            <HeaderStyle BackColor="#F5F1E8" />
                                                        </asp:GridView>
                                                        <asp:SqlDataSource ID="AwardsDS" runat="server" ConnectionString="<%$ ConnectionStrings:ProfilesDB %>"
                                                            DeleteCommand="usp_DeleteAward" DeleteCommandType="StoredProcedure" InsertCommand="usp_AddAward"
                                                            InsertCommandType="StoredProcedure" SelectCommand="usp_GetUserAwardsHonors" SelectCommandType="StoredProcedure"
                                                            UpdateCommand="usp_UpdateAward" UpdateCommandType="StoredProcedure">
                                                            <SelectParameters>
                                                                <asp:SessionParameter Name="PersonId" SessionField="ProfileUsername" Type="Int32" />
                                                            </SelectParameters>
                                                            <DeleteParameters>
                                                                <asp:Parameter Name="awardid" Type="Int32" />
                                                            </DeleteParameters>
                                                            <UpdateParameters>
                                                                <asp:Parameter Name="awardid" Type="Int32" />
                                                                <asp:Parameter Name="yr" Type="Int32" />
                                                                <asp:Parameter Name="yr2" Type="Int32" />
                                                                <asp:Parameter Name="awardnm" Type="String" />
                                                                <asp:Parameter Name="awardinginst" Type="String" />
                                                            </UpdateParameters>
                                                            <InsertParameters>
                                                                <asp:Parameter Name="PersonId" Type="String" />
                                                                <asp:Parameter Name="yr" Type="Int32" />
                                                                <asp:Parameter Name="yr2" Type="Int32" />
                                                                <asp:Parameter Name="awardnm" Type="String" />
                                                                <asp:Parameter Name="awardinginst" Type="String" />
                                                            </InsertParameters>
                                                        </asp:SqlDataSource>
                                                        <div>
                                                    </td>
                                                </tr>
                                            </table>
                                        </asp:Panel>
                                    </td>
                                </tr>
                                <tr style="height: 6px">
                                    <td>
                                    </td>
                                </tr>
                                <tr>
                                    <td>
                                        <asp:Panel ID="pnlEditNarrative" runat="server" CssClass="Borders">
                                            <table id="tblEditNarrative" width="100%">
                                                <tr>
                                                    <td style="text-align: left; white-space: nowrap;">
                                                        <h3>
                                                            Narrative</h3>
                                                    </td>
                                                    <td style="text-align: center; white-space: nowrap;">
                                                        <asp:Label ID="lblVisibleNarrative" runat="server" Text="This section is VISIBLE to the public"
                                                            ForeColor="#4faa4f" Font-Bold="true"></asp:Label>
                                                        <asp:Label ID="lblHiddenNarrative" runat="server" Text="This section is HIDDEN from the public"
                                                            ForeColor="#cc0000" Font-Bold="true"></asp:Label>
                                                    </td>
                                                    <td style="text-align: right; white-space: nowrap;">
                                                        <asp:LinkButton ID="btnHideNarrative" runat="server" OnClick="btnHideNarrative_OnClick">Hide</asp:LinkButton>
                                                        <asp:Label ID="lblHideNarrative" runat="server" Text="Hide" CssClass="NormalText"></asp:Label>
                                                        <b>&nbsp;|&nbsp;</b>
                                                        <asp:LinkButton ID="btnShowNarrative" runat="server" OnClick="btnShowNarrative_OnClick"
                                                            OnClientClick="JavaScript:return confirm('Are you sure you want to make this section visible to the public?')">Show</asp:LinkButton>
                                                        <asp:Label ID="lblShowNarrative" runat="server" Text="Show" CssClass="NormalText"></asp:Label>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td colspan="3">
                                                        <div style="padding: 10px 0px;">
                                                            <asp:ImageButton ID="btnImgEditNarrative" runat="server" ImageUrl="~/Images/icon_squareArrow.gif"
                                                                OnClick="btnEditNarrative_OnClick" />&nbsp;
                                                            <asp:LinkButton ID="btnEditNarrative" runat="server" OnClick="btnEditNarrative_OnClick"
                                                                CssClass="profileHypLinks">Edit Narrative</asp:LinkButton>
                                                        </div>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td colspan="3" style="text-align: justify;">
                                                        <asp:Label ID="lblEditNarrative" runat="server" />
                                                        <asp:Panel ID="pnlEditNarrativeText" runat="server" Style="background-color: #F0F4F6;
                                                            margin-bottom: 5px; border: solid 1px #999;" Visible="false" DefaultButton="btnSaveNarrative">
                                                            <div style="margin: 15px;">
                                                                <center>
                                                                    <asp:TextBox ID="txtEditNarrative" runat="server" TextMode="MultiLine" Rows="20"
                                                                        Width="100%"></asp:TextBox>
                                                                </center>
                                                                <div style="padding-top: 5px;">
                                                                    <asp:LinkButton ID="btnSaveNarrative" runat="server" OnClick="btnSaveNarrative_OnClick">Save</asp:LinkButton>
                                                                    &nbsp;&nbsp;<b>|</b>&nbsp;&nbsp;
                                                                    <asp:LinkButton ID="btnCancelNarrative" runat="server" OnClick="btnCancelNarrative_OnClick">Cancel</asp:LinkButton>
                                                                </div>
                                                            </div>
                                                        </asp:Panel>
                                                    </td>
                                                </tr>
                                            </table>
                                        </asp:Panel>
                                    </td>
                                </tr>
                                <tr style="height: 6px">
                                    <td>
                                    </td>
                                </tr>
                                <tr>
                                    <td>
                                        <asp:Panel ID="pnlEditPublications" runat="server" CssClass="Borders">
                                            <table id="tblEditPublications" width="100%">
                                                <tr>
                                                    <td style="text-align: left; white-space: nowrap;">
                                                        <h3>
                                                            Publications</h3>
                                                    </td>
                                                    <td style="text-align: center; white-space: nowrap;">
                                                        <asp:Label ID="lblVisiblePublication" runat="server" Text="This section is VISIBLE to the public"
                                                            ForeColor="#4faa4f" Font-Bold="true"></asp:Label>
                                                        <asp:Label ID="lblHiddenPublication" runat="server" Text="This section is HIDDEN from the public"
                                                            ForeColor="#cc0000" Font-Bold="true"></asp:Label>
                                                    </td>
                                                    <td style="text-align: right; white-space: nowrap;">
                                                        <asp:LinkButton ID="btnHidePublication" runat="server" OnClick="btnHidePublication_OnClick">Hide</asp:LinkButton>
                                                        <asp:Label ID="lblHidePublication" runat="server" Text="Hide" CssClass="NormalText"></asp:Label>
                                                        <b>&nbsp;|&nbsp;</b>
                                                        <asp:LinkButton ID="btnShowPublication" runat="server" OnClick="btnShowPublication_OnClick"
                                                            OnClientClick="JavaScript:return confirm('Are you sure you want to make this section visible to the public?')">Show</asp:LinkButton>
                                                        <asp:Label ID="lblShowPublication" runat="server" Text="Show" CssClass="NormalText"></asp:Label>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td colspan="3" style="padding-top: 10px;">
                                                        <asp:PlaceHolder ID="phAddPubMed" runat="server">
                                                            <div style="padding-bottom: 10px;">
                                                                <asp:ImageButton ID="btnImgAddPubMed" runat="server" ImageUrl="~/Images/icon_squareArrow.gif"
                                                                    OnClick="btnAddPubMed_OnClick" />&nbsp;
                                                                <asp:LinkButton ID="btnAddPubMed" runat="server" OnClick="btnAddPubMed_OnClick" CssClass="profileHypLinks">Add PubMed</asp:LinkButton>
                                                                &nbsp;(Search PubMed and add multiple articles.)
                                                            </div>
                                                        </asp:PlaceHolder>
                                                        <asp:PlaceHolder ID="phAddPub" runat="server">
                                                            <div style="padding-bottom: 10px;">
                                                                <asp:ImageButton ID="btnImgAddPub" runat="server" ImageUrl="~/Images/icon_squareArrow.gif"
                                                                    OnClick="btnAddPub_OnClick" />&nbsp;
                                                                <asp:LinkButton ID="btnAddPub" runat="server" OnClick="btnAddPub_OnClick" CssClass="profileHypLinks">Add by ID</asp:LinkButton>
                                                                &nbsp;(Add one or more articles using codes, e.g., PubMed ID.)
                                                            </div>
                                                        </asp:PlaceHolder>
                                                        <asp:PlaceHolder ID="phAddCustom" runat="server">
                                                            <div style="padding-bottom: 10px;">
                                                                <asp:ImageButton ID="btnImgAddCustom" runat="server" ImageUrl="~/Images/icon_squareArrow.gif"
                                                                    OnClick="btnAddCustom_OnClick" />&nbsp;
                                                                <asp:LinkButton ID="btnAddCustom" runat="server" OnClick="btnAddCustom_OnClick" CssClass="profileHypLinks">Add Custom</asp:LinkButton>
                                                                &nbsp;(Enter your own publication using an online form.)
                                                            </div>
                                                        </asp:PlaceHolder>
                                                        <asp:PlaceHolder ID="phDeletePub" runat="server">
                                                            <div style="padding-bottom: 10px;">
                                                                <asp:ImageButton ID="btnImgDeletePub" runat="server" ImageUrl="~/Images/icon_squareArrow.gif"
                                                                    OnClick="btnDeletePub_OnClick" />&nbsp;
                                                                <asp:LinkButton ID="btnDeletePub" runat="server" OnClick="btnDeletePub_OnClick" CssClass="profileHypLinks">Delete</asp:LinkButton>
                                                                &nbsp;(Remove multiple publications from your profile.)
                                                            </div>
                                                        </asp:PlaceHolder>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td colspan="3">
                                                        <%--Start Add By Id--%>
                                                        <asp:Panel ID="pnlAddPubById" runat="server" Style="background-color: #F0F4F6; margin-bottom: 5px;
                                                            border: solid 1px #999;" Visible="false" DefaultButton="btnSavePub">
                                                            <table border="0" cellspacing="2" cellpadding="4" width="100%">
                                                                <tr>
                                                                    <td>
                                                                        <div style="float: left; padding-right: 10px; padding-top: 3px;">
                                                                            <b>Enter one or more</b></div>
                                                                        <asp:DropDownList ID="drpPubIdType" runat="server" DataSourceID="PublicationTypeDS"
                                                                            DataTextField="name" DataValueField="pubidtype_id">
                                                                        </asp:DropDownList>
                                                                    </td>
                                                                    <td>
                                                                        <asp:TextBox ID="txtPubId" runat="server" TextMode="MultiLine" Rows="4" Columns="50"></asp:TextBox><br />
                                                                        (Separated by comma or semicolon, or one ID per line)
                                                                    </td>
                                                                </tr>
                                                                <tr>
                                                                    <td colspan="2">
                                                                        <div style="padding: 10px 0px;">
                                                                            <asp:LinkButton ID="btnSavePub" runat="server" CausesValidation="False" OnClick="btnSavePub_OnClick"
                                                                                Text="Save" TabIndex="5"></asp:LinkButton>
                                                                            &nbsp;&nbsp;|&nbsp;&nbsp;
                                                                            <asp:LinkButton ID="btnDonePub" runat="server" CausesValidation="False" OnClick="btnDonePub_OnClick"
                                                                                Text="Close" TabIndex="6"></asp:LinkButton>
                                                                        </div>
                                                                    </td>
                                                                </tr>
                                                            </table>
                                                            <asp:SqlDataSource ID="PublicationTypeDS" runat="server" ConnectionString="<%$ ConnectionStrings:ProfilesDB %>"
                                                                SelectCommand="SELECT [pubidtype_id], [name] FROM [pub_id_types] ORDER BY [sort_order]">
                                                            </asp:SqlDataSource>
                                                        </asp:Panel>
                                                        <%--End Add By Id--%>
                                                        <%--Start Add By Search--%>
                                                        <asp:Panel ID="pnlAddPubMed" runat="server" Style="background-color: #F0F4F6; margin-bottom: 5px;
                                                            border: solid 1px #999;" Visible="false" DefaultButton="btnPubMedSearch">
                                                            <div style="padding: 5px;">
                                                                <div>
                                                                    <b>Search PubMed</b>
                                                                </div>
                                                                <div style="padding-top: 10px;">
                                                                    <asp:RadioButton ID="rdoPubMedKeyword" GroupName="PubMedSearch" runat="server" Checked="true" />Enter
                                                                    author, affiliation or keyword in the field below.</div>
                                                                <div style="margin: 10px 0px 0px 25px;">
                                                                    <div style="padding-bottom: 10px;">
                                                                        <div style="width: 75px; float: left; text-align: right; padding-right: 10px; padding-top: 3px;">
                                                                            <b>Author(s)</b><br />
                                                                            (One per line)
                                                                        </div>
                                                                        <asp:TextBox ID="txtSearchAuthor" runat="server" TextMode="MultiLine" Rows="4" CssClass="textBoxBig"></asp:TextBox>
                                                                    </div>
                                                                    <div style="padding-bottom: 10px;">
                                                                        <div style="width: 75px; float: left; text-align: right; padding-right: 10px; padding-top: 3px;">
                                                                            <b>Affiliation</b>
                                                                        </div>
                                                                        <asp:TextBox ID="txtSearchAffiliation" runat="server" CssClass="textBoxBig"></asp:TextBox>&nbsp;&nbsp;<span
                                                                            style="color: #999;">Optional</span>
                                                                    </div>
                                                                    <%--<div style="padding-bottom: 10px;">
                                                                            <div style="width: 75px; float: left; text-align: right; padding-right: 10px; padding-top: 3px;">
                                                                                <b>Title</b>
                                                                            </div>
                                                                            <asp:TextBox ID="txtSearchTitle" runat="server" CssClass="textBoxBig"></asp:TextBox>&nbsp;&nbsp;<span style="color: #999;">Optional</span>
                                                                        </div>--%>
                                                                    <div>
                                                                        <div style="width: 75px; float: left; text-align: right; padding-right: 10px; padding-top: 3px;">
                                                                            <b>Keyword</b>
                                                                        </div>
                                                                        <asp:TextBox ID="txtSearchKeyword" runat="server" CssClass="textBoxBig"></asp:TextBox>&nbsp;&nbsp;<span
                                                                            style="color: #999;">Optional</span>
                                                                    </div>
                                                                </div>
                                                                <div style="padding-top: 10px;">
                                                                    <asp:RadioButton ID="rdoPubMedQuery" GroupName="PubMedSearch" runat="server" />Or
                                                                    you can also search by an arbitrary PubMed query in the field below.
                                                                </div>
                                                                <div style="margin: 10px 0px 0px 25px;">
                                                                    <div style="padding-bottom: 10px;">
                                                                        <div style="width: 75px; float: left; text-align: right; padding-right: 10px; padding-top: 3px;">
                                                                            <b>Query</b>
                                                                        </div>
                                                                        <asp:TextBox ID="txtPubMedQuery" runat="server" Style="width: 400px;"></asp:TextBox>
                                                                    </div>
                                                                </div>
                                                                <div style="padding-top: 10px;">
                                                                    <asp:CheckBox ID="chkPubMedExclude" runat="server" Checked="true" Text="Exclude articles already added to my profile." />
                                                                </div>
                                                                <div style="padding: 10px 0px;">
                                                                    <asp:LinkButton ID="btnPubMedSearch" runat="server" CausesValidation="False" OnClick="btnPubMedSearch_OnClick"
                                                                        Text="Search" TabIndex="5"></asp:LinkButton>
                                                                    &nbsp;&nbsp;|&nbsp;&nbsp;
                                                                    <asp:LinkButton ID="btnPubMedReset" runat="server" CausesValidation="False" OnClick="btnPubMedReset_OnClick"
                                                                        Text="Reset" TabIndex="6"></asp:LinkButton>
                                                                    &nbsp;&nbsp;|&nbsp;&nbsp;
                                                                    <asp:LinkButton ID="btnPubMedClose" runat="server" CausesValidation="False" OnClick="btnPubMedClose_OnClick"
                                                                        Text="Close" TabIndex="6"></asp:LinkButton>
                                                                </div>
                                                            </div>
                                                        </asp:Panel>
                                                        <%--End Add By Search--%>
                                                        <%--Start Search Results--%>
                                                        <asp:Panel ID="pnlAddPubMedResults" runat="server" Style="background-color: #F0F4F6;
                                                            margin-bottom: 5px; border: solid 1px #999;" Visible="false">
                                                            <div style="padding: 5px;">
                                                                <div>
                                                                    <div style="width: 25px; float: left;">
                                                                        <asp:Image ID="Image1" runat="server" ImageUrl="~/Images/icon_alert.gif" />
                                                                    </div>
                                                                    <div style="margin-left: 25px;">
                                                                        Check the articles that are yours in the list below, and then click the Add Selected
                                                                        link at the bottom of the page.
                                                                    </div>
                                                                </div>
                                                                <div style="padding: 10px 0px 5px 0px;">
                                                                    <asp:Label ID="lblPubMedResultsHeader" runat="server" Text="" Style="font-weight: bold;"></asp:Label>
                                                                </div>
                                                                <div style="padding: 10px 0px 5px 5px; background-color: #E2E6E8;">
                                                                    <b>Select:</b>&nbsp;&nbsp;
                                                                    <asp:LinkButton ID="btnSelectAll" runat="server" CausesValidation="False" OnClick="btnSelectAll_OnClick"
                                                                        Text="All" CssClass="profileHypLinks"></asp:LinkButton>
                                                                    &nbsp;&nbsp;|&nbsp;&nbsp;
                                                                    <asp:LinkButton ID="btnSelectNone" runat="server" CausesValidation="False" OnClick="btnSelectNone_OnClick"
                                                                        Text="None" CssClass="profileHypLinks"></asp:LinkButton>
                                                                </div>
                                                                <div>
                                                                    <asp:GridView ID="grdPubMedSearchResults" runat="server" GridLines="None" EmptyDataText="No PubMed Publications Found."
                                                                        DataKeyNames="pmid" AutoGenerateColumns="false" AllowPaging="false" PageSize="10"
                                                                        OnRowDataBound="grdPubMedSearchResults_RowDataBound" OnPageIndexChanging="grdPubMedSearchResults_PageIndexChanging"
                                                                        CellPadding="4">
                                                                        <PagerSettings Position="TopAndBottom" LastPageText="&gt;&gt;" FirstPageText="&lt;&lt;"
                                                                            Mode="NumericFirstLast" />
                                                                        <Columns>
                                                                            <asp:TemplateField>
                                                                                <ItemTemplate>
                                                                                    <asp:CheckBox ID="chkPubMed" runat="server" />
                                                                                </ItemTemplate>
                                                                            </asp:TemplateField>
                                                                            <asp:TemplateField>
                                                                                <ItemTemplate>
                                                                                    <asp:Label ID="lblCitation" Text='<%#Eval("citation") %>' runat="server" />
                                                                                </ItemTemplate>
                                                                            </asp:TemplateField>
                                                                        </Columns>
                                                                    </asp:GridView>
                                                                </div>
                                                                <div style="padding: 10px 0px 5px 5px; background-color: #E2E6E8;">
                                                                    <asp:LinkButton ID="btnPubMedAddSelected" runat="server" CausesValidation="False"
                                                                        OnClick="btnPubMedAddSelected_OnClick" Text="Add Selected" CssClass="profileHypLinks"></asp:LinkButton>
                                                                </div>
                                                            </div>
                                                        </asp:Panel>
                                                        <%--End Search Results--%>
                                                        <%--Start Custom Publication--%>
                                                        <asp:Panel ID="pnlAddCustomPubMed" runat="server" Style="background-color: #F0F4F6;
                                                            margin-bottom: 5px; border: solid 1px #999;" Visible="false" DefaultButton="btnAddPub">
                                                            <div style="padding: 5px;">
                                                                <div>
                                                                    <b>Select the type of publication you would like to add</b>&nbsp;&nbsp;
                                                                    <asp:DropDownList ID="drpPublicationType" runat="server" AutoPostBack="true" OnSelectedIndexChanged="drpPublicationType_SelectedIndexChanged">
                                                                        <asp:ListItem Value="" Text="--Select--"></asp:ListItem>
                                                                        <asp:ListItem>Abstracts</asp:ListItem>
                                                                        <asp:ListItem>Books/Monographs/Textbooks</asp:ListItem>
                                                                        <asp:ListItem>Clinical Communications</asp:ListItem>
                                                                        <asp:ListItem>Educational Materials</asp:ListItem>
                                                                        <asp:ListItem>Non-Print Materials</asp:ListItem>
                                                                        <asp:ListItem>Original Articles</asp:ListItem>
                                                                        <asp:ListItem>Patents</asp:ListItem>
                                                                        <asp:ListItem>Proceedings of Meetings</asp:ListItem>
                                                                        <asp:ListItem>Reviews/Chapters/Editorials</asp:ListItem>
                                                                        <asp:ListItem>Thesis</asp:ListItem>
                                                                    </asp:DropDownList>
                                                                </div>
                                                                <div style="padding: 5px 0px 0px 0px;">
                                                                    (Check if your publication is in
                                                                    <asp:LinkButton ID="btnPubMedById" runat="server" CssClass="profileHypLinks" OnClick="btnPubMedById_Click">PubMed</asp:LinkButton>
                                                                    before manually entering it.)
                                                                </div>
                                                                <div style="padding: 15px 0px 5px 0px;">
                                                                    <asp:LinkButton ID="btnPubMedCutomClose" runat="server" CausesValidation="False"
                                                                        OnClick="btnPubMedFinished_OnClick" Text="Close"></asp:LinkButton>
                                                                </div>
                                                                <asp:PlaceHolder Visible="false" ID="phMain" runat="server">
                                                                    <hr />
                                                                    <div style="padding-top: 5px;">
                                                                        <b>Author(s)</b> Enter the name of all the authors as they appear in the publication.<br />
                                                                        <asp:TextBox ID="txtPubMedAuthors" runat="server" TextMode="MultiLine" Rows="4" CssClass="textBoxBigger"></asp:TextBox>
                                                                    </div>
                                                                    <div class="pubHeader">
                                                                        <asp:Label ID="lblTitle" runat="server" Text=""></asp:Label>
                                                                    </div>
                                                                    <asp:TextBox ID="txtPubMedTitle" runat="server" CssClass="textBoxBigger"></asp:TextBox>
                                                                    <asp:PlaceHolder Visible="false" ID="phTitle2" runat="server">
                                                                        <div class="pubHeader">
                                                                            <asp:Label ID="lblTitle2" runat="server" Text=""></asp:Label>
                                                                        </div>
                                                                        <asp:TextBox ID="txtPubMedTitle2" runat="server" CssClass="textBoxBigger"></asp:TextBox>
                                                                    </asp:PlaceHolder>
                                                                    <asp:PlaceHolder Visible="false" ID="phEdition" runat="server">
                                                                        <div class="pubSubSpacer">
                                                                            <asp:Label ID="Label8" runat="server" Text="Edition" CssClass="pubSubHeader"></asp:Label>
                                                                        </div>
                                                                        <asp:TextBox ID="txtPubMedEdition" runat="server"></asp:TextBox>
                                                                    </asp:PlaceHolder>
                                                                    <div class="pubHeader">
                                                                        Publication Information
                                                                    </div>
                                                                    <div class="pubSubSpacer">
                                                                        <div style="float: left; padding-right: 20px;">
                                                                            <asp:Label ID="Label9" runat="server" Text="Date" CssClass="pubSubHeader"></asp:Label><br />
                                                                            <asp:TextBox ID="txtPubMedPublicationDate" runat="server" MaxLength="10" CssClass="textBoxSmall"></asp:TextBox>
                                                                            <asp:ImageButton ID="btnCalendar" runat="server" ImageUrl="~/Images/cal.gif" />
                                                                            <ajaxtoolkit:CalendarExtender ID="CalendarExtender1" runat="server" TargetControlID="txtPubMedPublicationDate"
                                                                                PopupButtonID="btnCalendar">
                                                                            </ajaxtoolkit:CalendarExtender>
                                                                        </div>
                                                                        <asp:PlaceHolder Visible="false" ID="phPubIssue" runat="server">
                                                                            <div style="float: left; padding-right: 20px;">
                                                                                <asp:Label ID="Label10" runat="server" Text="Issue" CssClass="pubSubHeader"></asp:Label><br />
                                                                                <asp:TextBox ID="txtPubMedPublicationIssue" runat="server" MaxLength="10" CssClass="textBoxSmall"></asp:TextBox>
                                                                            </div>
                                                                        </asp:PlaceHolder>
                                                                        <asp:PlaceHolder Visible="false" ID="phPubVolume" runat="server">
                                                                            <div style="float: left; padding-right: 20px;">
                                                                                <asp:Label ID="Label11" runat="server" Text="Volume" CssClass="pubSubHeader"></asp:Label><br />
                                                                                <asp:TextBox ID="txtPubMedPublicationVolume" runat="server" MaxLength="10" CssClass="textBoxSmall"></asp:TextBox>
                                                                            </div>
                                                                        </asp:PlaceHolder>
                                                                        <asp:PlaceHolder Visible="false" ID="phPubPageNumbers" runat="server">
                                                                            <div>
                                                                                <asp:Label ID="Label12" runat="server" Text="Page Numbers" CssClass="pubSubHeader"></asp:Label><br />
                                                                                <asp:TextBox ID="txtPubMedPublicationPages" runat="server"></asp:TextBox>
                                                                            </div>
                                                                        </asp:PlaceHolder>
                                                                    </div>
                                                                    <div style="clear: left;">
                                                                    </div>
                                                                    <asp:PlaceHolder Visible="false" ID="phNewsSection" runat="server">
                                                                        <div style="clear: left; padding: 20px 0px 5px 0px;">
                                                                            If the item was published in a newspaper, enter the following information.
                                                                        </div>
                                                                        <div class="pubSubSpacer">
                                                                            <div style="float: left; padding-right: 20px;">
                                                                                <asp:Label ID="Label13" runat="server" Text="Section" CssClass="pubSubHeader"></asp:Label><br />
                                                                                <asp:TextBox ID="txtPubMedNewsSection" runat="server"></asp:TextBox>
                                                                            </div>
                                                                            <div>
                                                                                <asp:Label ID="Label14" runat="server" Text="Column" CssClass="pubSubHeader"></asp:Label><br />
                                                                                <asp:TextBox ID="txtPubMedNewsColumn" runat="server"></asp:TextBox>
                                                                            </div>
                                                                        </div>
                                                                    </asp:PlaceHolder>
                                                                    <asp:PlaceHolder Visible="false" ID="phNewsUniversity" runat="server">
                                                                        <div class="pubSubSpacer">
                                                                            <div style="float: left; padding-right: 20px;">
                                                                                <asp:Label ID="Label15" runat="server" Text="University" CssClass="pubSubHeader"></asp:Label><br />
                                                                                <asp:TextBox ID="txtPubMedNewsUniversity" runat="server" CssClass="textBoxBig"></asp:TextBox>
                                                                            </div>
                                                                            <div>
                                                                                <asp:Label ID="Label16" runat="server" Text="City" CssClass="pubSubHeader"></asp:Label><br />
                                                                                <asp:TextBox ID="txtPubMedNewsCity" runat="server"></asp:TextBox>
                                                                            </div>
                                                                        </div>
                                                                    </asp:PlaceHolder>
                                                                    <asp:PlaceHolder Visible="false" ID="phPublisherInfo" runat="server">
                                                                        <div class="pubHeader">
                                                                            Publisher Information
                                                                        </div>
                                                                    </asp:PlaceHolder>
                                                                    <asp:PlaceHolder Visible="false" ID="phPublisherName" runat="server">
                                                                        <div class="pubSubSpacer">
                                                                            <div style="float: left; padding-right: 20px;">
                                                                                <asp:Label ID="Label19" runat="server" Text="Name" CssClass="pubSubHeader"></asp:Label><br />
                                                                                <asp:TextBox ID="txtPubMedPublisherName" runat="server" CssClass="textBoxBig"></asp:TextBox>
                                                                            </div>
                                                                            <div>
                                                                                <asp:Label ID="Label20" runat="server" Text="City" CssClass="pubSubHeader"></asp:Label><br />
                                                                                <asp:TextBox ID="txtPubMedPublisherCity" runat="server"></asp:TextBox>
                                                                            </div>
                                                                        </div>
                                                                    </asp:PlaceHolder>
                                                                    <asp:PlaceHolder Visible="false" ID="phPublisherNumbers" runat="server">
                                                                        <div class="pubSubSpacer">
                                                                            <div style="float: left; padding-right: 20px;">
                                                                                <asp:Label ID="lblPubMedPublisherReport" runat="server" Text="Report Number" CssClass="pubSubHeader"></asp:Label><br />
                                                                                <asp:TextBox ID="txtPubMedPublisherReport" runat="server" CssClass="textBoxBig"></asp:TextBox>
                                                                            </div>
                                                                            <div>
                                                                                <asp:Label ID="lblPubMedPublisherContract" runat="server" Text="Contract Number"
                                                                                    CssClass="pubSubHeader"></asp:Label><br />
                                                                                <asp:TextBox ID="txtPubMedPublisherContract" runat="server" CssClass="textBoxBig"></asp:TextBox>
                                                                            </div>
                                                                        </div>
                                                                    </asp:PlaceHolder>
                                                                    <asp:PlaceHolder Visible="false" ID="phConferenceInfo" runat="server">
                                                                        <div class="pubHeader">
                                                                            Conference Information
                                                                        </div>
                                                                        <div class="pubSubSpacer">
                                                                            <asp:Label ID="Label23" runat="server" Text="Conference Edition(s)" CssClass="pubSubHeader"></asp:Label><br />
                                                                            <asp:TextBox ID="txtPubMedConferenceEdition" runat="server" TextMode="MultiLine"
                                                                                Rows="4" CssClass="textBoxBigger"></asp:TextBox>
                                                                        </div>
                                                                        <div class="pubSubSpacer">
                                                                            <asp:Label ID="Label24" runat="server" Text="Conference Name" CssClass="pubSubHeader"></asp:Label><br />
                                                                            <asp:TextBox ID="txtPubMedConferenceName" runat="server" TextMode="MultiLine" Rows="4"
                                                                                CssClass="textBoxBigger"></asp:TextBox>
                                                                        </div>
                                                                        <div class="pubSubSpacer">
                                                                            <div style="float: left; padding-right: 20px;">
                                                                                <asp:Label ID="Label25" runat="server" Text="Conference Dates" CssClass="pubSubHeader"></asp:Label><br />
                                                                                <asp:TextBox ID="txtPubMedConferenceDate" runat="server" CssClass="textBoxBig"></asp:TextBox>
                                                                            </div>
                                                                            <div>
                                                                                <asp:Label ID="Label26" runat="server" Text="Location" CssClass="pubSubHeader"></asp:Label><br />
                                                                                <asp:TextBox ID="txtPubMedConferenceLocation" runat="server" CssClass="textBoxBig"></asp:TextBox>
                                                                            </div>
                                                                        </div>
                                                                    </asp:PlaceHolder>
                                                                    <asp:PlaceHolder Visible="false" ID="phAdditionalInfo" runat="server">
                                                                        <div class="pubHeader">
                                                                            Additional Information
                                                                        </div>
                                                                        <asp:TextBox ID="txtPubMedAdditionalInfo" runat="server" TextMode="MultiLine" Rows="4"
                                                                            CssClass="textBoxBigger"></asp:TextBox>
                                                                    </asp:PlaceHolder>
                                                                    <asp:PlaceHolder Visible="false" ID="phAdditionalInfo2" runat="server">
                                                                        <div style="color: #666666; padding-top: 5px;">
                                                                            <asp:Label ID="lblAdditionalInfo" runat="server" Text="Label"></asp:Label>
                                                                        </div>
                                                                    </asp:PlaceHolder>
                                                                    <div style="padding-top: 20px;">
                                                                        <b>Abstract</b> (Optional)<br />
                                                                        <asp:TextBox ID="txtPubMedAbstract" runat="server" TextMode="MultiLine" Rows="4"
                                                                            CssClass="textBoxBigger"></asp:TextBox>
                                                                    </div>
                                                                    <div style="padding-top: 20px;">
                                                                        <b>Website URL</b> (Optional) Clicking the citation title will take the user to
                                                                        this website.<br />
                                                                        <asp:TextBox ID="txtPubMedOptionalWebsite" runat="server" CssClass="textBoxBigger"></asp:TextBox>
                                                                    </div>
                                                                    <div style="padding: 10px 0px;">
                                                                        <asp:LinkButton ID="btnPubMedSaveCustomAdd" runat="server" CausesValidation="False"
                                                                            OnClick="btnPubMedSaveCustom_OnClick" Text="Save and add another"></asp:LinkButton>
                                                                        &nbsp;&nbsp;|&nbsp;&nbsp;
                                                                        <asp:LinkButton ID="btnPubMedSaveCustom" runat="server" CausesValidation="False"
                                                                            OnClick="btnPubMedSaveCustom_OnClick" Text="Save and close"></asp:LinkButton>
                                                                        &nbsp;&nbsp;|&nbsp;&nbsp;
                                                                        <asp:LinkButton ID="btnPubMedFinished" runat="server" CausesValidation="False" OnClick="btnPubMedFinished_OnClick"
                                                                            Text="Close"></asp:LinkButton>
                                                                    </div>
                                                                </asp:PlaceHolder>
                                                            </div>
                                                        </asp:Panel>
                                                        <%--End Custom Publication--%>
                                                        <%--Start Delete Publications--%>
                                                        <asp:Panel ID="pnlDeletePubMed" runat="server" Style="background-color: #F0F4F6;
                                                            margin-bottom: 5px; border: solid 1px #999;" Visible="false" DefaultButton="btnSavePub">
                                                            <div style="padding: 5px;">
                                                                <div>
                                                                    To delete a single publication, click the X to the right of the citation. To delete
                                                                    multiple publications, select one of the options below. Note that you cannot undo
                                                                    this!
                                                                </div>
                                                                <div style="padding: 10px 0px;">
                                                                    <asp:LinkButton ID="btnDeletePubMedOnly" runat="server" CausesValidation="False"
                                                                        OnClick="btnDeletePubMedOnly_OnClick" Text="Delete only PubMed citations" OnClientClick="Javascript:return confirm('Are you sure you want to delete the PubMed citations?');"></asp:LinkButton>
                                                                    &nbsp;&nbsp;|&nbsp;&nbsp;
                                                                    <asp:LinkButton ID="btnDeleteCustomOnly" runat="server" CausesValidation="False"
                                                                        OnClick="btnDeleteCustomOnly_OnClick" Text="Delete only custom citations" OnClientClick="Javascript:return confirm('Are you sure you want to delete the Custom citations?');"></asp:LinkButton>
                                                                    &nbsp;&nbsp;|&nbsp;&nbsp;
                                                                    <asp:LinkButton ID="btnDeleteAll" runat="server" CausesValidation="False" OnClick="btnDeleteAll_OnClick"
                                                                        Text="Delete all citations" OnClientClick="Javascript:return confirm('Are you sure you want to delete all citations?');"></asp:LinkButton>
                                                                    &nbsp;&nbsp;|&nbsp;&nbsp;
                                                                    <asp:LinkButton ID="btnDeletePubMedClose" runat="server" CausesValidation="False"
                                                                        OnClick="btnDeletePubMedClose_OnClick" Text="Close"></asp:LinkButton>
                                                                </div>
                                                            </div>
                                                        </asp:Panel>
                                                        <%--End Delete Publications--%>
                                                        <%--Start Publications List--%>
                                                        <asp:GridView ID="grdEditPublications" runat="server" AutoGenerateColumns="False"
                                                            GridLines="Horizontal" HorizontalAlign="Left" EmptyDataText="&lt;div style=&quot;font-style:italic; color:#999; &quot;&gt;None&lt;/div&gt;"
                                                            OnRowDeleted="grdEditPublications_RowDeleted" CellPadding="4" DataSourceID="PublicationDS"
                                                            Width="100%" DataKeyNames="PubID" OnRowDataBound="grdEditPublications_RowDataBound"
                                                            OnSelectedIndexChanged="grdEditPublications_SelectedIndexChanged" PageSize="15">
                                                            <Columns>
                                                                <asp:TemplateField ShowHeader="false" ItemStyle-HorizontalAlign="Right" HeaderStyle-Width="5%">
                                                                    <ItemTemplate>
                                                                        <asp:Label ID="Label5" runat="server" Text='<%# Bind("RowNum") %>'></asp:Label>
                                                                        <asp:HiddenField ID="hdnFromPubMed" runat="server" Value='<%# Bind("FromPubMed") %>' />
                                                                    </ItemTemplate>
                                                                    <HeaderStyle Width="5%" />
                                                                    <ItemStyle HorizontalAlign="Right" />
                                                                </asp:TemplateField>
                                                                <asp:BoundField DataField="Reference" HeaderText="Citations" ReadOnly="true" SortExpression="Reference" />
                                                                <asp:TemplateField ShowHeader="False">
                                                                    <ItemTemplate>
                                                                        <asp:LinkButton ID="btnEditPublication" runat="server" Visible="false" CausesValidation="False"
                                                                            CommandName="Select" Text="Edit"></asp:LinkButton>
                                                                    </ItemTemplate>
                                                                    <ItemStyle HorizontalAlign="Center" Width="25px" />
                                                                </asp:TemplateField>
                                                                <asp:TemplateField ShowHeader="False">
                                                                    <ItemTemplate>
                                                                        <asp:LinkButton ID="LinkButton4" runat="server" CausesValidation="False" CommandName="Delete"
                                                                            OnClientClick="Javascript:return confirm('Are you sure you want to delete this citation?');"
                                                                            Text="X"></asp:LinkButton>
                                                                        <asp:HiddenField ID="hdnMPID" runat="server" Value='<%# Bind("mpid") %>' />
                                                                        <asp:HiddenField ID="hdnPMID" runat="server" Value='<%# Bind("pmid") %>' />
                                                                    </ItemTemplate>
                                                                    <ItemStyle HorizontalAlign="Center" Width="10px" />
                                                                </asp:TemplateField>
                                                            </Columns>
                                                            <HeaderStyle BackColor="#F5F1E8" />
                                                            <SelectedRowStyle BackColor="#F0F4F6" />
                                                        </asp:GridView>
                                                        <asp:SqlDataSource ID="PublicationDS" runat="server" ConnectionString="<%$ ConnectionStrings:ProfilesDB %>"
                                                            DeleteCommand="usp_DeletePublication" DeleteCommandType="StoredProcedure" SelectCommand="usp_GetUserPublications"
                                                            SelectCommandType="StoredProcedure" ProviderName="<%$ ConnectionStrings:ProfilesDB.ProviderName %>">
                                                            <SelectParameters>
                                                                <asp:QueryStringParameter Name="UserId" Type="String" QueryStringField="Person" />
                                                            </SelectParameters>
                                                            <DeleteParameters>
                                                                <asp:SessionParameter Name="PersonId" SessionField="ProfileUsername" Type="Int32" />
                                                                <asp:Parameter Name="PubID" Type="String" />
                                                            </DeleteParameters>
                                                        </asp:SqlDataSource>
                                                        <%--End Publications List--%>
                                                    </td>
                                                </tr>
                                            </table>
                                        </asp:Panel>
                                    </td>
                                </tr>
                            </table>
                        </div>
                    </ContentTemplate>
                </asp:UpdatePanel>

	          <%-- Profiles OpenSocial Extension by UCSF --%>    
                <asp:Panel ID="pnlOpenSocialGadgets" runat="server">
                    <script type="text/javascript" language="javascript">
                        my.current_view = "home";
                    </script>                
                    <div id="OpenSocial">
                        <table width="100%">
                            <tr style="height: 6px">
                                <td>
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    <asp:Panel ID="pnlOpenSocialGadgetsInner" runat="server" CssClass="Borders" >
                                        <table width="100%">
                                            <tr>
                                                <td style="text-align: left;">
                                                    <h3>Add more to your profile <span style="font-weight:normal"> with websites related to your research, information about Faculty Mentorship <em>(Student Mentorship information coming soon)</em> and even post your presentations here.</span></h3>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td>
                                                    <div id="gadgets-edit" class="gadgets-gadget-parent"></div>
                                                </td>
                                            </tr>
                                        </table>
                                    </asp:Panel>
                                </td>
                            </tr>
                        </table>
                    </div>
                </asp:Panel>
                
                <asp:Panel ID="pnlControl" runat="server" Visible="false">
                    <asp:PlaceHolder ID="plToControl" runat="server" />
                </asp:Panel>
                <asp:HiddenField ID="hdnValue" runat="server" Visible="true" />
            </td>
        </tr>
    </table>
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
