/*
 * Copyright (c) 2010--2014 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package com.redhat.rhn.frontend.taglibs;

import com.redhat.rhn.common.localization.LocalizationService;
import com.redhat.rhn.domain.rhnset.RhnSet;
import com.redhat.rhn.domain.user.User;
import com.redhat.rhn.frontend.struts.RequestContext;
import com.redhat.rhn.frontend.struts.SessionSetHelper;
import com.redhat.rhn.manager.rhnset.RhnSetDecl;
import com.redhat.rhn.manager.rhnset.RhnSetManager;

import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;

import java.util.Arrays;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;


/**
 * DWRItemSelector
 */
public class DWRItemSelector {
    public static final String JSON_HEADER = "X-JSON";
    public static final String IDS = "ids";
    public static final String CHECKED = "checked";
    public static final String SET_LABEL = "set_label";

    /**
     * Dwr Item selector updates the RHNset
     * when its passed the setLabel, and ids to update
     * @param setLabel the set label
     * @param ids the ids to update
     * @param on true if the items were to be added
     * @return the selected
     * @throws Exception on exceptions
     */
    public String select(String setLabel, String[] ids, boolean on) throws Exception {
        WebContext ctx = WebContextFactory.get();
        HttpServletRequest req = ctx.getHttpServletRequest();
        User user = new RequestContext(req).getCurrentUser();
        Integer size = updateSetFromRequest(req, setLabel, ids, on, user);
        if (size == null) {
            return "";
        }
        return getResponse(size, setLabel);
    }

    /**
     * Update the proper set based upon request parameters
     *
     * @param req servlet request
     * @param setLabel set label
     * @param which ids to toggle
     * @param isOn true to add, false to remove
     * @param user the user updating the set
     * @return the final count of items in the set
     *
     * @throws Exception if anything bad happens
     */
    public static Integer updateSetFromRequest(HttpServletRequest req,
            String setLabel, String[] which, boolean isOn, User user) throws Exception {
        if (which == null) {
            return null;
        }

        if (SessionSetHelper.exists(req, setLabel)) {
            Set<String> set  = SessionSetHelper.lookupAndBind(req, setLabel);

            if (isOn) {
                set.addAll(Arrays.asList(which));
            }
            else {
                for (String id : which) {
                    set.remove(id);
                }
            }
            return set.size();
        }
        RhnSetDecl decl = RhnSetDecl.find(setLabel);
        if (decl != null) {
            RhnSet set = decl.get(user);
            if (isOn) {
                set.addElements(which);
            }
            else {
                set.removeElements(which);
            }
            RhnSetManager.store(set);
            return set.size();
        }
        return null;
    }


    // Write an responseText with the current count from the set
    private String getResponse(int setSize, String setLabel) {
        StringBuilder responseText = new StringBuilder();
        LocalizationService ls = LocalizationService.getInstance();
        Boolean systemsRelated = RhnSetDecl.SYSTEMS.getLabel().equals(setLabel);
        if (systemsRelated) {
            StringBuilder headerMessage = new StringBuilder();
            headerMessage.append("<span id='spacewalk-set-system_list-counter'")
                         .append(" class='badge'>")
                         .append(Integer.toString(setSize))
                         .append("</span>");
            if (setSize == 1) {
                headerMessage.append(ls.getMessage("header.jsp.singleSystemSelected"));
            }
            else {
                headerMessage.append(ls.getMessage("header.jsp.systemsSelected"));
            }
            responseText.append("\"header\":\"").append(headerMessage).append("\"");
        }

        if (responseText.length() > 0) {
            responseText.append(",");
        }

        String paginationMessage = "";
        if (!systemsRelated) {
            paginationMessage = ls.getMessage("message.numselected",
                    Integer.toString(setSize));
        }
        responseText.append("\"pagination\":\"")
                    .append(paginationMessage)
                    .append("\"");
        return  "({" + responseText.toString() + "})";
    }
}
