<%@page contentType="text/html"
   import="javax.naming.*,javax.transaction.*"
%>
<%
   InitialContext ctx = new InitialContext();
   UserTransaction ut = (UserTransaction) ctx.lookup("java:jboss/UserTransaction");
   ut.begin();
%>
