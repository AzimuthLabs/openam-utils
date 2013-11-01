<%@ page language="java" contentType="text/csv; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.lang.reflect.*" %>
<%@ page import="java.text.*" %>
<%@ page import="com.iplanet.dpro.session.*" %>
<%@ page import="com.iplanet.dpro.session.service.*" %>
<%@ page import="java.util.*" %>
<%@ page import="com.sun.identity.common.*" %>
<%@ page import="com.iplanet.am.util.*" %>

<%
        // allow only local access
        if ( !request.getLocalAddr().equals( request.getRemoteAddr() ) ) {
            response.sendError( 401 );
            return;
        }

        long startTime = System.currentTimeMillis();
        
        Date             currentTime          = new Date();
        SimpleDateFormat formatter            = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String           formattedCurrentTime = formatter.format( currentTime );
        
        String         serverName     = SystemProperties.get("com.iplanet.am.server.host");
        SessionService sessionService = SessionService.getSessionService();
        
        // expose the field!
        Class< ? extends SessionService >  sessionServiceClazz = sessionService.getClass();
        Field                              sessionTableField   = sessionServiceClazz.getDeclaredField( "sessionTable" );
        //Class< ? extends InternalSession > sessionClazz        = InternalSession.class;
        //Method                             isAppSessionMethod  = sessionClazz.getDeclaredMethod( "isAppSession" );
        
        sessionTableField.setAccessible( true ); // show it
        //isAppSessionMethod.setAccessible( true ); // show it
        
        Hashtable< SessionID, InternalSession > sessionTable = ( Hashtable< SessionID, InternalSession > )sessionTableField.get( sessionService );
        
        //sessionTableField.setAccessible( false ); // hide it back
        //isAppSessionMethod.setAccessible( false ); // hide it back

        // start counting
        Map<String, Integer> counts = new HashMap<String, Integer>();
        
        synchronized( sessionTable )
        {
            Enumeration< InternalSession > e = sessionTable.elements();
            while ( e.hasMoreElements() ) {
                InternalSession s = ( InternalSession )e.nextElement();
                // we care only for application sessions (e.g. Policy Agent session)
                //if( (s.getState() == 1) && (Boolean)isAppSessionMethod.invoke( s ) ) { // not using this as max idle timeout for agents might be changed!
                if( (s.getState() == 1) && s.getMaxSessionTime() == 153722867280912930L ) {
                    String name = s.getClientID();
                    
                    int lastCount = 0;
                    
                    if ( counts.containsKey( name ) ) {
                        lastCount = counts.get( name );
                    }

                    counts.put( name, lastCount + 1 );
                }
            }
        }
        
        // print out all!
%># Retrieving all application sessions from server: <%= serverName %> at <%= formattedCurrentTime %>
<%
		for ( String name : counts.keySet() ) {
            int count = counts.get( name );
            %><%= formattedCurrentTime %>,<%= serverName %>,<%= name %>,<%= count %>
<%
        }
        
        long endTime   = System.currentTimeMillis();
        long totalTime = endTime - startTime;
%># Total took <%= totalTime %> milliseconds
