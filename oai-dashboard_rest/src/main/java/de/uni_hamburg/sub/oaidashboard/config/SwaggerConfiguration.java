package de.uni_hamburg.sub.oaidashboard.config;

import io.swagger.jaxrs.config.BeanConfig;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

public class SwaggerConfiguration extends HttpServlet {
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setTitle("OAI-Dashboard-REST-API");
        beanConfig.setVersion("1.0");
        beanConfig.setSchemes(new String[]{"http"});
        beanConfig.setBasePath(config.getServletContext().getContextPath() + "/rest");
        beanConfig.setResourcePackage("de.uni_hamburg.sub.oaidashboard.rest");
        beanConfig.setScan(true);
        beanConfig.setDescription("REST-API for accessing harvested metadata of OA-Repositories");
    }
}