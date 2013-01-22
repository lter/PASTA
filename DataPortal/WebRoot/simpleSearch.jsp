<%@ page language="java" import="java.util.*" pageEncoding="ISO-8859-1"%>
<%@ page import="edu.lternet.pasta.portal.search.LTERTerms" %>
<%
  String path = request.getContextPath();
  String basePath = request.getScheme() + "://" + request.getServerName()
      + ":" + request.getServerPort() + path + "/";

  String searchResult = (String) request.getAttribute("searchresult");

  if (searchResult == null)
    searchResult = "";

  String jqueryString = LTERTerms.getJQueryString(); // for auto-complete using JQuery
%>

<!doctype html>
<html>
<head>
<base href="<%=basePath%>">

<title>Simple Search</title>

<meta http-equiv="pragma" content="no-cache">
<meta http-equiv="cache-control" content="no-cache">
<meta http-equiv="expires" content="0">
<meta http-equiv="keywords" content="keyword1,keyword2,keyword3">
<meta http-equiv="description" content="This is my page">

<link rel="stylesheet" type="text/css" href="./css/lter-nis.css">
<link rel="stylesheet" href="http://code.jquery.com/ui/1.10.0/themes/base/jquery-ui.css" />

<jsp:include page="/WEB-INF/jsp/javascript.jsp" />

<script src="http://code.jquery.com/ui/1.10.0/jquery-ui.js"></script>
<script>
  $(function() {
    var availableTags = [ <%=jqueryString%> ];
    
    $( "#lterterms" ).autocomplete({
        source: availableTags
    });
  });
</script>  
</head>

<body>

	<div class="wrapper">

		<jsp:include page="/WEB-INF/jsp/head.jsp" />
		<jsp:include page="/WEB-INF/jsp/menuTopLevel.jsp" />

		<div class="content">

			<h2 align="center">Search for Data Packages</h2>

			<fieldset>
				<legend>Basic Search</legend>

				<p>Search for data packages using one or more terms separated by
					spaces</p>

				<div class="section">
					<form id="simplesearch" name="simplesearch" method="post"
						action="./simpleSearch">
						<table align="left" cellpadding="4em">
							<tbody>
								<tr>
									<td align="left" width="260px">
									  <label for="terms">Search Terms (use * for any):</label>
									</td>
									<td align="left" width="200px">
									  <div class="ui-widget">
									    <input type="search" name="terms" required="required" size="60" id="lterterms"/>
									  </div>
								  </td>
									<td align="center" width="70px"><input type="submit"
										name="search" value="search" />
									</td>
									<td align="center" width="40px"><input type="reset"
										name="reset" value="reset" />
									</td>
									<td>(<a target="_top" href="./advancedSearch.jsp">Advanced Search</a>)</td>
								</tr>
							</tbody>
						</table>
					</form>
				</div>
			</fieldset>

			<div class="section-table">
				<%=searchResult%>
			</div>
			<!-- end of section-table -->

		</div>
		<!-- end of content -->

		<jsp:include page="/WEB-INF/jsp/foot.jsp" />

	</div>
	<!-- end of wrapper -->

</body>
</html>
