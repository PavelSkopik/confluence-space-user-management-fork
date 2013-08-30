/**
 * Copyright (c) 2007-2013, Custom Space User Management Plugin Development Team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Custom Space User Management Plugin Development Team
 *       nor the names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package csum.confluence.permissionmgmt.service.impl;

import com.atlassian.confluence.security.SpacePermission;
import com.atlassian.confluence.security.SpacePermissionManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.crowd.embedded.api.CrowdDirectoryService;
import com.atlassian.crowd.embedded.api.CrowdService;
import com.atlassian.user.Group;
import com.atlassian.user.GroupManager;
import com.atlassian.user.UserManager;
import com.atlassian.user.search.page.DefaultPager;
import com.atlassian.user.search.page.Pager;
import csum.confluence.permissionmgmt.config.CustomPermissionConfiguration;
import csum.confluence.permissionmgmt.service.GroupManagementService;
import csum.confluence.permissionmgmt.service.exception.FindException;
import csum.confluence.permissionmgmt.service.vo.ServiceContext;
import csum.confluence.permissionmgmt.util.StringUtil;
import csum.confluence.permissionmgmt.util.group.GroupNameUtil;
import csum.confluence.permissionmgmt.util.group.GroupUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Rajendra Kadam
 * @author Gary S. Weaver
 */
public abstract class BaseGroupManagementService extends UserAndGroupManagementService implements GroupManagementService {

    // assuming these are autowired
    protected SpacePermissionManager spacePermissionManager;
    protected CustomPermissionConfiguration customPermissionConfiguration;

    @Autowired
    public BaseGroupManagementService(SpacePermissionManager spacePermissionManager,
                                      CrowdService crowdService,
                                      CustomPermissionConfiguration customPermissionConfiguration,
                                      GroupManager groupManager,
                                      CrowdDirectoryService crowdDirectoryService,
                                      UserAccessor userAccessor) {
        super(crowdService, crowdDirectoryService, groupManager, userAccessor);
        this.spacePermissionManager = spacePermissionManager;
        this.customPermissionConfiguration = customPermissionConfiguration;

        if (spacePermissionManager==null) {
			throw new RuntimeException("spacePermissionManager was not autowired in BaseGroupManagementService");
        }
        else if (customPermissionConfiguration==null) {
			throw new RuntimeException("customPermissionConfiguration was not autowired in BaseGroupManagementService");
        }
    }

    public Pager findGroups(ServiceContext context) throws FindException {
        log.debug("findGroups() called");
        Map mapWithGroupnamesAsKeys = getGroupsWithViewspacePermissionAsKeysAsMapWithGroupnamesAsKeys(context);
        List groups = getReadWriteGroupsThatMatchNamePatternExcludingConfluenceAdministrators(mapWithGroupnamesAsKeys, context);
        GroupUtil.sortGroupsByGroupnameAscending(groups);
        Pager pager = new DefaultPager(groups);
        return pager;
    }

    public boolean isAllowedToManageGroup(ServiceContext context, String groupName) throws FindException {
        log.debug("isAllowedToManageGroup() called. groupName=" + groupName);
        Map mapWithGroupnamesAsKeys = getGroupsWithViewspacePermissionAsKeysAsMapWithGroupnamesAsKeys(context);
        List groupNames = getReadWriteGroupnamesThatMatchNamePatternExcludingConfluenceAdministrators(mapWithGroupnamesAsKeys, context);
        return groupNames.contains(groupName);
    }

    private List getReadWriteGroupsThatMatchNamePatternExcludingConfluenceAdministrators(Map mapWithGroupnamesAsKeys, ServiceContext context) {
        log.debug("getGroupsThatMatchNamePatternExcludingConfluenceAdministrators() called");
        List groupNames = getReadWriteGroupnamesThatMatchNamePatternExcludingConfluenceAdministrators(mapWithGroupnamesAsKeys, context);
        List groups = new ArrayList();
        for (int i = 0; i < groupNames.size(); i++) {
            String groupName = (String) groupNames.get(i);
            Group group = getGroup(groupName);
            if (isGroupReadOnly(group)) {
                log.debug("group '" + groupName + "' is read-only according to Confluence, therefore it cannot be managed by CSUM.");
            } else {
                groups.add(group);
            }
        }
        return groups;
    }

    // When managing via Jira API, all groups are readonly in Confluence API, but not via SOAP, necessarily.
    protected abstract boolean isGroupReadOnly(Group group);

    private List getReadWriteGroupnamesThatMatchNamePatternExcludingConfluenceAdministrators(Map mapWithGroupnamesAsKeys, ServiceContext context) {
        log.debug("getGroupsThatMatchNamePatternExcludingConfluenceAdministrators() called");
        List groupNames = new ArrayList();

        ArrayList notAllowedUser = new ArrayList();
        notAllowedUser.add("confluence-administrators");

        CustomPermissionConfiguration config = getCustomPermissionConfiguration();
        String spaceKey = context.getSpace().getKey();
        String prefix = GroupNameUtil.replaceSpaceKey(config.getNewGroupNameCreationPrefixPattern(), spaceKey);
        String suffix = GroupNameUtil.replaceSpaceKey(config.getNewGroupNameCreationSuffixPattern(), spaceKey);

        for (Iterator iterator = mapWithGroupnamesAsKeys.keySet().iterator(); iterator.hasNext();) {
            String groupName = (String) iterator.next();
            //If notAllowedUser doesn't contain this group name
            //and group name matches the pattern, then only add this user-group for display.
            //log.debug("Selected Groups .....");
            boolean isPatternMatch = GroupNameUtil.doesGroupMatchPattern(groupName, prefix, suffix);
            if ((!notAllowedUser.contains(groupName)) && isPatternMatch) {
                //log.debug("Group '" + grpName + "' allowed and matched pattern " + pat.pattern() );
                groupNames.add(groupName);
            } else {
                //log.debug("Group '" + grpName + "' not allowed or didn't match pattern. notAllowedUser=" + StringUtil.convertCollectionToCommaDelimitedString(notAllowedUser) + " isPatternMatch=" + isPatternMatch + " pattern=" + pat.pattern());
            }
            //log.debug("-------End of Groups---------");

        }
        return groupNames;
    }

    private Map getGroupsWithViewspacePermissionAsKeysAsMapWithGroupnamesAsKeys(ServiceContext context) {
        log.debug("getGroupsWithViewspacePermissionAsKeysAsMapWithGroupnamesAsKeys() called");
        Space space = context.getSpace();
        //VIEWSPACE_PERMISSION is basic permission that every user group can have.
        Map map = spacePermissionManager.getGroupsForPermissionType(SpacePermission.VIEWSPACE_PERMISSION, space);
        if (map == null || map.size() == 0) {
            log.debug("No groups with permissiontype SpacePermission.VIEWSPACE_PERMISSION");
        } else {
            log.debug("Got the following groups with permissiontype SpacePermission.VIEWSPACE_PERMISSION: " + StringUtil.convertCollectionToCommaDelimitedString(map.keySet()));
        }
        return map;
    }

    public CustomPermissionConfiguration getCustomPermissionConfiguration() {
        return customPermissionConfiguration;
    }
}
