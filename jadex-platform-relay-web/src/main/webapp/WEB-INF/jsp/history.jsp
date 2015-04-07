<%@page session="false"%>
<jsp:include page="header.jsp">
	<jsp:param name="title" value="Relay Transport - Connection History" />	
</jsp:include>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="jadex.base.relay.*" %>
<%@ page import="java.util.*" %>
<%
	PlatformInfo[]	infos	= (PlatformInfo[])request.getAttribute("platforms");
%>


<%
if(infos.length>0)
{
	Map<String, String> markers	= new LinkedHashMap<String, String>();
	for(int i=0; i<infos.length; i++) 
	{
		if(infos[i].getPosition()!=null)
		{
			String	marker	= markers.get(infos[i].getPosition());
			if(marker==null)
			{
				marker	= "<h3>"+infos[i].getLocation()+"</h3>";
			}
			else
			{
				marker	+= "<br/>";
			}
			marker	+= (i+1)+": "+infos[i].getId()+" ("+infos[i].getHostName()+")";
			markers.put(infos[i].getPosition(), marker);
		}
	}
	
	if(markers.size()>0)
	{ %>
		<!-- map styles: examples.map-i86nkdio, examples.map-qfyrx5r8 -->
		<div id="mapcontainer">
			<div id="map">Please wait while the history is loading...</div>
		</div>
		<script type="text/javascript">
			var addressPoints = [
		    <% for(Map.Entry<String, String> marker: markers.entrySet()) { %>       
				[<%= marker.getKey()%>, "<%= marker.getValue() %>"],
			<% } %>
			];
			var tiles = L.tileLayer('http://{s}.tiles.mapbox.com/v3/examples.map-i86nkdio/{z}/{x}/{y}.png', {
					minZoom: 2,
					maxZoom: 18,
					attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery � <a href="http://mapbox.com">Mapbox</a>'
				}),
				latlng = L.latLng(53.550556, 9.993333);
	
			var map = L.map('map', {center: latlng, zoom: 13, layers: [tiles]});
	
			var markers = L.markerClusterGroup({ chunkedLoading: true });
			
			for (var i = 0; i < addressPoints.length; i++) {
				var a = addressPoints[i];
				var title = a[2];
				var marker = L.marker(L.latLng(a[0], a[1]), { title: title });
				marker.bindPopup(title);
				markers.addLayer(marker);
			}
	
			map.addLayer(markers);
			map.fitBounds(markers.getBounds());
			
			$("#mapcontainer").on("resizestop", function(event, ui)
			{
				var mapwidth	= $("#mapcontainer").outerWidth(true); // Map size with margins, padding, etc.
				var parentwidth	= $("#mapcontainer").parent().width();
				if(mapwidth>parentwidth)
				{
					var gap	= mapwidth - $("#mapcontainer").width(); // Calc. size of margins, padding, etc.
					$("#mapcontainer").width(parentwidth-gap);			
				}
				map.invalidateSize();
			});
		</script>
<%	}
} %>

<table>
	<tr>
		<th>&nbsp;</th>
		<th>&nbsp;</th>
		<th>Platform</th>
		<th>Host</th>
		<th>Location</th>
		<th>Last Seen</th>
		<th>First Appeared</th>		
		<th># Seen</th>
	</tr>
	
	<%
		for(int i=0; i<infos.length; i++)
		{%>
			<tr class="<%= i%2==0 ? "even" : "odd" %>" title="<%= infos[i].toString() %>">
				<td>
					<%= i+1 %>
					</td>
				<td>
					<% if(infos[i].getCountryCode()!=null) {%>
						<img src="<%= request.getContextPath() %>/resources/flags/flag-<%= infos[i].getCountryCode() %>.png"/>
					<% } %>
					</td>
				<td>
					<%= infos[i].getId() %> </td>
				<td>
					<%= infos[i].getHostName() %></td>
				<td>
					<%= infos[i].getLocation() %></td>
				<td>
					<%= infos[i].getConnectTime() %></td>
				<td>
					<%= infos[i].getDisconnectTime() %></td>
				<td class="number">
					<%= infos[i].getMessageCount() %>
			</tr>
	<%	} %>
</table>

<jsp:include page="footer.jsp" flush="true"/>
