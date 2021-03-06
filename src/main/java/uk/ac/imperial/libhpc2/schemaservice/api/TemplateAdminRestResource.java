/*
 * Copyright (c) 2015, Imperial College London
 * Copyright (c) 2015, The University of Edinburgh
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the names of the copyright holders nor the names of their
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * -----------------------------------------------------------------------------
 *
 * This file is part of the TemPSS - Templates and Profiles for Scientific 
 * Software - service, developed as part of the libhpc projects 
 * (http://www.imperial.ac.uk/lesc/projects/libhpc).
 *
 * We gratefully acknowledge the Engineering and Physical Sciences Research
 * Council (EPSRC) for their support of the projects:
 *   - libhpc: Intelligent Component-based Development of HPC Applications
 *     (EP/I030239/1).
 *   - libhpc Stage II: A Long-term Solution for the Usability, Maintainability
 *     and Sustainability of HPC Software (EP/K038788/1).
 */

package uk.ac.imperial.libhpc2.schemaservice.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.RandomStringUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import uk.ac.imperial.libhpc2.schemaservice.TempssObject;
import uk.ac.imperial.libhpc2.schemaservice.TempssTemplateLoader;
import uk.ac.imperial.libhpc2.schemaservice.web.db.TempssUser;



/**
 * Jersey REST class representing the admin/template endpoint
 * for undertaking template administration tasks such as adding
 * new templates and constraints.
 * @author jhc02
 *
 */
@Component
@Path("admin/template")
public class TemplateAdminRestResource {

    /**
     * Logger
     */
    private static final Logger sLog = LoggerFactory.getLogger(TemplateAdminRestResource.class.getName());
    
    /**
     * ServletContext object used to access template data
     * Injected via @Context annotation
     */
    ServletContext _context;

    @Context
    public void setServletContext(ServletContext pContext) {
        this._context = pContext;
        sLog.debug("Servlet context injected: " + pContext);
    }

    /**
     * /admin/template/[templateName] requests are used to add or update templates.
     * A PUT request is used to add a new template. This requires that the
     * template configuration file is uploaded with the request. If a template 
     * with the specified [templateName] already exists, this will fail.
     * 
     * A POST request is used to update an existing template. If a template 
     * with the specified [templateName] doesn't exist, this will fail. If it does, 
     * it will be replaced with the new content provided with the POST request.
     * 
     */
    
    @SuppressWarnings("unchecked")
	// Add a new template - we'll be passed up to three files in the upload form.
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("application/json")
    public Response addNewTemplate(
    		@Context HttpServletRequest pRequest,
    		@FormDataParam("templateNewName") String newName,
    		@FormDataParam("templateNewId") String newId,
    		@FormDataParam("templateNewGroup") String newGroup,
    		@FormDataParam("templateCurrentId") String currentId,
    		@FormDataParam("templateCurrentName") String currentName,
    		@FormDataParam("file-schema") InputStream uploadFileSchema,
    		@FormDataParam("file-schema") FormDataContentDisposition uploadFileInfoSchema,
    		@FormDataParam("file-transform") InputStream uploadFileTransform,
    		@FormDataParam("file-transform") FormDataContentDisposition uploadFileInfoTransform,
    		@FormDataParam("file-constraint") InputStream uploadFileConstraint,
    		@FormDataParam("file-constraint") FormDataContentDisposition uploadFileInfoConstraint,
            FormDataMultiPart multipartData) {
    	
    	TempssUser user = ApiUtils.getAuthenticatedUser();
    	if(user == null) {
    		String responseText = "{\"status\":\"ERROR\", \"code\":\"AUTHENTICATION_REQUIRED\", \"error\":" +
					"\"You must be authenticated to add a new template.\"}";
			return Response.status(Status.FORBIDDEN).entity(responseText).build();
    	}
    	
    	Map<String, TempssObject> components = (Map<String, TempssObject>)_context.getAttribute("components");
    	
    	boolean updateTemplate = false;
    	String templId = null;
    	String templName = null;
    	if( newId != null && !newId.equals("") && newName != null && !newName.equals("") ) {
    		sLog.debug("We are adding a new template with name <{}> and ID <{}>.", newName, newId);
    		templId = newId;
    		templName = newName;
    		if(components.containsKey(templId)) {
    			String responseText = "{\"status\":\"ERROR\", \"code\":\"TEMPLATE_EXISTS\", \"error\":" +
    					"\"A template with the specified ID already exists.\"}";
        		return Response.status(Status.CONFLICT).entity(responseText).build();
    		}
    	}
    	else if(currentName != null && !currentName.equals("") && currentId != null && !currentId.equals("")) {
    		updateTemplate = true;
    		sLog.debug("We are updating existing template with id <{}> and name <{}>.", currentId, currentName);
    		templId = currentId;
    		int spacerIdx = currentName.indexOf(" - ");
    		if(spacerIdx >= 0) {
    			currentName = currentName.substring(spacerIdx+3);
    		}
    		templId = currentId;
    		templName = currentName;
    	}
    	else {
    		String responseText = "{\"status\":\"ERROR\", \"code\":\"MISSING_PARAMS\", \"error\":" +
					"\"Details of either an existing template to update or a new template are missing.\"}";
			return Response.status(Status.BAD_REQUEST).entity(responseText).build();
    	}
    	
    	if(uploadFileSchema == null || uploadFileInfoSchema == null) {
    		String responseText = "{\"status\":\"ERROR\", \"code\":\"MISSING_FILE\", \"error\":" +
					"\"Required file upload for new/updated template is missing.\"}";
			return Response.status(Status.BAD_REQUEST).entity(responseText).build();
    	}
    	
    	// Get the parameters from the request and see if we're updating a template or creating
    	// a new one.
    	sLog.debug("Received file upload for new/updated template. Filename <{}>, new ID <{}>, new name <{}>, " +
    			"original name <{}>.", uploadFileInfoSchema.getFileName(), newId, newName, currentName);
    	
    	// If we have access to all the data required, we now build the necessary structures
    	// to store the template
    	if(!Files.exists(TempssTemplateLoader.TEMPLATE_STORE_DIR)) {
    		try {
    			Files.createDirectories(TempssTemplateLoader.TEMPLATE_STORE_DIR);
    			Files.createDirectory(TempssTemplateLoader.TEMPLATE_STORE_DIR.resolve("Template"));
    			Files.createDirectory(TempssTemplateLoader.TEMPLATE_STORE_DIR.resolve("Schema"));
    			Files.createDirectory(TempssTemplateLoader.TEMPLATE_STORE_DIR.resolve("Transform"));
    			Files.createDirectory(TempssTemplateLoader.TEMPLATE_STORE_DIR.resolve("Constraints"));
    		} catch(IOException e) {
    			String responseText = "{\"status\":\"ERROR\", \"code\":\"CREATE_DIR_ERROR\", \"error\":" +
    					"\"Unable to create missing template store directory.\"}";
    			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(responseText).build();
    		}
    	}
    	
    	String randStr = RandomStringUtils.randomAlphanumeric(8);
    	
    	String name = uploadFileInfoSchema.getFileName();
    	int dotIdx = name.indexOf('.');
    	String schemaFile = (dotIdx >= 0) ? 
    			name.substring(0, dotIdx) + "_" + randStr + name.substring(dotIdx) : 
    				name + "_" + randStr;
    	
    	String transformFile = null;
    	if(uploadFileTransform != null) {
    		sLog.debug("We have a transform file uploaded - handling the transform file...");
    		String transformName = uploadFileInfoTransform.getFileName();
        	dotIdx = transformName.indexOf('.');
        	transformFile = (dotIdx >= 0) ? 
        			transformName.substring(0, dotIdx) + "_" + randStr + transformName.substring(dotIdx) : 
        				transformName + "_" + randStr; 
    	}
    	String constraintFile = null;
    	if(uploadFileConstraint != null) {
    		sLog.debug("We have a constraints file uploaded - handling the constraints file...");
    		String constraintName = uploadFileInfoConstraint.getFileName();
        	dotIdx = constraintName.indexOf('.');
        	constraintFile = (dotIdx >= 0) ? 
        			constraintName.substring(0, dotIdx) + "_" + randStr + constraintName.substring(dotIdx) : 
        				constraintName + "_" + randStr; 
    	}
    			
		// Get a handle for the properties file - required to check if it exists.
		File templPropsFile = new File(TempssTemplateLoader.TEMPLATE_STORE_DIR.resolve("Template").resolve(
				"template-" + templId + ".properties").toString());
    	/*
    	 * FIXME: Fix how we handle files that we're saving.
    	 * For now, if we're updating an existing template, we overwrite its properties file
    	 **/
    	if(templPropsFile.exists() && !updateTemplate) {
    		String responseText = "{\"status\":\"ERROR\", \"code\":\"TEMPLATE_FILE_EXISTS\", \"error\":" +
					"\"The template properties file already exists.\"}";
    		return Response.status(Status.CONFLICT).entity(responseText).build();
    	}
    	else if(templPropsFile.exists()) {
    		sLog.debug("Overwriting previous properties file <{}> for template.", templPropsFile.getAbsolutePath());
    	}

    	// **** TODO: Handle the transform and constraint files when setting up the properties file....
    	
    	// We now need to prepare the properties file that defines the template files.
    	Properties templProps = new Properties();
    	templProps.put("component.id", templId);
    	templProps.put(templId + ".name", templName);
    	templProps.put(templId + ".schema", schemaFile);
    	templProps.put(templId + ".transform", (transformFile != null) ? transformFile : "");
    	if(constraintFile != null) {
    		templProps.put(templId + ".constraints", constraintFile);
    	}
    	if(newGroup != null && !newGroup.equals("")) {
    		templProps.put(templId + ".group", newGroup);
    	}
    	
    	// Get the path for the schema file
    	String schemaPath = TempssTemplateLoader.TEMPLATE_STORE_DIR.resolve("Schema").resolve(schemaFile).toString();
    	
    	// Now write the properties file out to the Template directory
    	try {
    		FileOutputStream fos = new FileOutputStream(templPropsFile);
    		templProps.store(fos, "Template metadata for uploaded TemPSS template.");
    		fos.close();
    	} catch(FileNotFoundException e) {
    		String responseText = "{\"status\":\"ERROR\", \"code\":\"FILE_CREATE_ERROR\", \"error\":" +
					"\"Unable to create or write to the file storing this template metadata.\"}";
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(responseText).build();
    	} catch (IOException e) {
    		String responseText = "{\"status\":\"ERROR\", \"code\":\"METADATA_WRITE_ERROR\", \"error\":" +
					"\"Unable to store the templ file storing this template metadata.\"}";
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(responseText).build();		
    	}
    	
    	// Now store the uploaded file to the Schema directory
    	int bytesCopied = -1;
    	int transformBytesCopied = -1;
    	int constraintBytesCopied = -1;
    	try {
    		bytesCopied = FileCopyUtils.copy(uploadFileSchema, new FileOutputStream(schemaPath));
		} catch (IOException e) {
			String responseText = "{\"status\":\"ERROR\", \"code\":\"SCHEMA_WRITE_ERROR\", " +
					"\"error\":\"Unable to store the uploaded schema file.\"}";
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(responseText).build();
		}
    	
    	if(transformFile != null) {
    		// Store the transform file
    		try {
    			String transformPath = TempssTemplateLoader.TEMPLATE_STORE_DIR.resolve("Transform").resolve(transformFile).toString();
        		transformBytesCopied = FileCopyUtils.copy(uploadFileTransform, new FileOutputStream(transformPath));
    		} catch (IOException e) {
    			String responseText = "{\"status\":\"ERROR\", \"code\":\"TRANSFORM_WRITE_ERROR\", " +
    					"\"error\":\"Unable to store the uploaded transform file.\"}";
        		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(responseText).build();
    		}
    	}
    	
    	if(constraintFile != null) {
    		// Store the constraint file
    		try {
    			String constraintPath = TempssTemplateLoader.TEMPLATE_STORE_DIR.resolve("Constraints").resolve(constraintFile).toString();
        		constraintBytesCopied = FileCopyUtils.copy(uploadFileConstraint, new FileOutputStream(constraintPath));
    		} catch (IOException e) {
    			String responseText = "{\"status\":\"ERROR\", \"code\":\"CONSTRAINTS_WRITE_ERROR\", " +
    					"\"error\":\"Unable to store the uploaded constraints file.\"}";
        		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(responseText).build();
    		}
    	}
    	
    	JSONObject response = new JSONObject();
    	try {
    		JSONArray arr = new JSONArray();
        	JSONObject fileObj = new JSONObject();
			fileObj.put("name", uploadFileInfoSchema.getFileName());
			fileObj.put("size", bytesCopied);
	    	fileObj.put("url", "");
	    	arr.put(fileObj);
	    	
	    	if(transformFile != null) {
	    		fileObj = new JSONObject();
				fileObj.put("name", uploadFileInfoTransform.getFileName());
				fileObj.put("size", transformBytesCopied);
		    	fileObj.put("url", "");
		    	arr.put(fileObj);
	    	}
	    	
	    	if(constraintFile != null) {
	    		fileObj = new JSONObject();
				fileObj.put("name", uploadFileInfoConstraint.getFileName());
				fileObj.put("size", constraintBytesCopied);
		    	fileObj.put("url", "");
		    	arr.put(fileObj);
	    	}
	    	
	    	response.put("files", arr);
	    	response.put("result", "OK");
		} catch (JSONException e) {
			String responseText = "{\"status\":\"ERROR\", \"code\":\"RESPONSE_ERROR\", \"error\":" +
					"\"Unable to build response data.\"}";
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(responseText).build();
		}
    	
    	// Before the response is returned, we need to update the component
    	// data to include the newly added or updated template...
    	String objTransform = (transformFile != null) ? transformFile : "";
    	TempssObject tempssObj = new TempssObject(templId, templName, schemaFile, objTransform, constraintFile);
    	tempssObj.setPath(TempssTemplateLoader.TEMPLATE_STORE_DIR.toString());
    	if(newGroup != null && !newGroup.equals("")) {
    		tempssObj.setGroup(newGroup);
    	}
    	components.put(templId, tempssObj);
    	
    	return Response.ok(response.toString(), MediaType.APPLICATION_JSON).build();
    }

    // Update an existing template
//    @POST
//    @Produces("application/json")
//    public Response updateExistingTemplate(
//    		@Context HttpServletRequest pRequest,
//            FormDataMultiPart multipartData) {
//        
//        return Response.ok(null, MediaType.APPLICATION_JSON).build();
//    }

    
}
