/*
 * Copyright 2005-2006 Noelios Consulting.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * http://www.opensource.org/licenses/cddl1.txt
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * http://www.opensource.org/licenses/cddl1.txt
 * If applicable, add the following below this CDDL
 * HEADER, with the fields enclosed by brackets "[]"
 * replaced with your own identifying information:
 * Portions Copyright [yyyy] [name of copyright owner]
 */

package com.noelios.restlet.application;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Filter;
import org.restlet.Restlet;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.spi.Helper;

import com.noelios.restlet.LogFilter;

/**
 * Application implementation.
 * @author Jerome Louvel (contact@noelios.com) <a href="http://www.noelios.com/">Noelios Consulting</a>
 */
public class ApplicationHelper implements Helper
{
	/** The application to help. */
	private Application application;

	/** The first Restlet. */
	private Restlet first;

	/** The parent context, typically the container's context. */
	private Context parentContext;

	/**
	 * Constructor.
	 * @param application The application to help.
	 * @param parentContext The parent context, typically the container's context.
	 */
	public ApplicationHelper(Application application, Context parentContext)
	{
		this.application = application;
		this.parentContext = parentContext;
		this.first = null;
	}

	/**
	 * Creates a new context.
	 * @return The new context.
	 */
	public Context createContext()
	{
		String loggerName = getApplication().getLogService().getContextLoggerName();

		if (loggerName == null)
		{
			loggerName = Application.class.getCanonicalName() + "."
					+ getApplication().getName() + "(" + getApplication().hashCode() + ")";
		}

		return new ApplicationContext(getApplication(), getParentContext(), Logger
				.getLogger(loggerName));
	}

	/**
	 * Allows filtering before processing by the next Restlet. Does nothing by default.
	 * @param request The request to handle.
	 * @param response The response to update.
	 */
	public void handle(Request request, Response response)
	{
		if (getFirst() != null)
		{
			getFirst().handle(request, response);
		}
		else
		{
			response.setStatus(Status.SERVER_ERROR_INTERNAL);
			getApplication().getLogger().log(Level.SEVERE,
					"The application wasn't properly started, it can't handle calls.");
		}
	}

	/**
	 * Returns the application to help.
	 * @return The application to help.
	 */
	public Application getApplication()
	{
		return this.application;
	}

	/**
	 * Returns the parent context, typically the container's context.
	 * @return The parent context.
	 */
	public Context getParentContext()
	{
		return this.parentContext;
	}

	/** Start hook. */
	public void start() throws Exception
	{
		Filter lastFilter = null;

		// Logging of calls
		if (getApplication().getLogService().isEnabled())
		{
			lastFilter = createLogFilter(getApplication().getContext(), getApplication()
					.getLogService().getAccessLoggerName(), getApplication().getLogService()
					.getAccessLogFormat());
			setFirst(lastFilter);
		}

		// Addition of status pages
		if (getApplication().getStatusService().isEnabled())
		{
			Filter statusFilter = createStatusFilter(getApplication());
			if (lastFilter != null) lastFilter.setNext(statusFilter);
			if (getFirst() == null) setFirst(statusFilter);
			lastFilter = statusFilter;
		}

		// Addition of decoder filter
		if (getApplication().getDecoderService().isEnabled())
		{
			Filter decoderFilter = createDecoderFilter(getApplication());
			if (lastFilter != null) lastFilter.setNext(decoderFilter);
			if (getFirst() == null) setFirst(decoderFilter);
			lastFilter = decoderFilter;
		}

		// Reattach the original filter's attached Restlet
		if (getFirst() == null)
		{
			setFirst(getApplication().getRoot());
		}
		else
		{
			lastFilter.setNext(getApplication().getRoot());
		}
	}

	/**
	 * Creates a new log filter. Allows overriding.
	 * @param context The context.
	 * @param logName The log name to used in the logging.properties file.
	 * @param logFormat The log format to use.
	 * @return The new log filter.
	 */
	protected Filter createLogFilter(Context context, String logName, String logFormat)
	{
		return new LogFilter(context, logName, logFormat);
	}

	/**
	 * Creates a new decoder filter. Allows overriding.
	 * @param application The parent application.
	 * @return The new decoder filter.
	 */
	protected Filter createDecoderFilter(Application application)
	{
		return new DecoderFilter(application.getContext(), true, false);
	}

	/**
	 * Creates a new status filter. Allows overriding.
	 * @param application The parent application.
	 * @return The new status filter.
	 */
	protected Filter createStatusFilter(Application application)
	{
		return new ApplicationStatusFilter(application);
	}

	/** Stop callback. */
	public void stop() throws Exception
	{

	}

	/**
	 * Returns the first Restlet.
	 * @return the first Restlet.
	 */
	private Restlet getFirst()
	{
		return this.first;
	}

	/**
	 * Sets the first Restlet.
	 * @param first The first Restlet.
	 */
	private void setFirst(Restlet first)
	{
		this.first = first;
	}

}
