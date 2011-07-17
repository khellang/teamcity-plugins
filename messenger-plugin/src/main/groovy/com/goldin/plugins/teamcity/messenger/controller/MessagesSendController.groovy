package com.goldin.plugins.teamcity.messenger.controller

import com.goldin.plugins.teamcity.messenger.api.Message
import com.goldin.plugins.teamcity.messenger.api.Message.Urgency
import com.goldin.plugins.teamcity.messenger.api.MessagesBean
import com.goldin.plugins.teamcity.messenger.api.MessagesContext
import com.goldin.plugins.teamcity.messenger.api.MessagesUtil
import jetbrains.buildServer.serverSide.SBuildServer
import jetbrains.buildServer.users.SUser
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.springframework.web.servlet.ModelAndView
import com.goldin.plugins.teamcity.messenger.api.MessagesConfiguration

/**
 * Controller activated when message is sent
 */
class MessagesSendController extends MessagesBaseController
{
    static final String MAPPING = 'messagesSend.html'


    @Requires({ server && manager && messagesBean && context && config && util })
    MessagesSendController ( SBuildServer          server,
                             WebControllerManager  manager,
                             MessagesBean          messagesBean,
                             MessagesContext       context,
                             MessagesConfiguration config,
                             MessagesUtil          util )
    {
        super( server, messagesBean, context, config, util )
        manager.registerController( "/$MAPPING", this )
    }


    @Requires({ requestParams && user && username })
    @Ensures({ result != null })
    ModelAndView handleRequest ( Map<String, ?> requestParams, SUser user, String username )
    {
        Urgency      urgency       = param( requestParams, 'urgency' ).toUpperCase() as Urgency
        String       messageText   = param( requestParams, 'message' )
        long         longevity     = longevity( requestParams ) ?: 1
        boolean      sendToAll     = param( requestParams,  'all',    false ) as boolean
        List<String> sendToGroups  = params( requestParams, 'groups', false ).collect { util.htmlUnescape( it )} // Values are not sent
        List<String> sendToUsers   = params( requestParams, 'users',  false ).collect { util.htmlUnescape( it )} // when groups/users are disabled
        Message      message       = new Message( username, urgency, messageText, longevity, sendToAll, sendToGroups, sendToUsers )
        long         messageId     = messagesBean.sendMessage( message )

        new TextModelAndView( messageId as String, context.locale )
    }


    /**
     * Retrieves message "longevity" in hours, for how long should it be kept in the system.
     *
     * @param requestParams current request parameters
     * @return message "longevity" in hours
     */
    @Requires({ requestParams })
    private long longevity ( Map<String, ?> requestParams )
    {
        float  number  = ( param( requestParams, 'longevity-number', false ) ?: -1 ) as float
        String unit    = param( requestParams, 'longevity-unit' )

        assert number != 0.0

        number * (( 'hours'  == unit ) ? 1       :
                  ( 'days'   == unit ) ? 24      :
                  ( 'weeks'  == unit ) ? 24 * 7  :
                  ( 'months' == unit ) ? 24 * 30 :
                                         24 * 365 )
    }
}