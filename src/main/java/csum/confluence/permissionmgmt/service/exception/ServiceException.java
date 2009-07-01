/**
 * Copyright (c) 2007-2009, Custom Space User Management Plugin Development Team
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

package csum.confluence.permissionmgmt.service.exception;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Gary S. Weaver
 */
public class ServiceException extends Exception {

    List throwables = new ArrayList();

    public ServiceException() {
        super();
    }

    public ServiceException(String string) {
        super(string);
    }

    public ServiceException(String string, Throwable throwable) {
        super(string, throwable);
    }

    public ServiceException(Throwable throwable) {
        super(throwable);
    }


    public List getThrowables() {
        return throwables;
    }

    public String getThrowablesMessagesAsCommaDelimitedString() {
        StringBuffer sb = new StringBuffer();
        if (this.throwables != null) {
            Iterator iter = this.throwables.iterator();
            int count = 0;
            while (iter.hasNext()) {
                Throwable throwable = (Throwable) iter.next();
                if (count != 0) {
                    sb.append(", ");
                }
                sb.append(throwable.getMessage());
                count++;
            }
        }
        return sb.toString();
    }

    public void setThrowables(List throwables) {
        this.throwables = throwables;
    }

    public void addThrowable(Throwable t) {
        throwables.add(t);
    }
}
