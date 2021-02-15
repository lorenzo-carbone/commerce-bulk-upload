<%@ include file="/init.jsp" %>
<portlet:actionURL var="userCSVDataUploadURL" name="<%=MVCCommandNames.UPLOAD_CSV%>"></portlet:actionURL>

<c:set var = "templateURL" scope = "session" value = '<%=request.getContextPath() + "/media/product_import.csv"%>'/>
<p>
	<b><liferay-ui:message key="commercebulkupload.caption"/></b>
</p>
<a href="${templateURL}" target="_blank">Download Template</a>
<aui:form action="${userCSVDataUploadURL}" enctype="multipart/form-data" method="post" id="csvDataFileForm">
    <div>
        <label>Upload Product Data CSV :</label>
        <input type="file" name='<portlet:namespace/>csvDataFile' id="csvDataFile"></input>
    </div>
    <aui:button-row>
        <aui:button type="submit" name="submit-cart" value="Upload" />
    </aui:button-row>
</aui:form>