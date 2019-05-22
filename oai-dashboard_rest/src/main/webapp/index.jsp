<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>API UI</title>
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/webjars/swagger-ui/3.22.1/swagger-ui.css" >
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/webjars/swagger-ui/3.22.1/favicon-32x32.png" sizes="32x32" />
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/webjars/swagger-ui/3.22.1/favicon-16x16.png" sizes="16x16" />
    <style>
      html
      {
        box-sizing: border-box;
        overflow: -moz-scrollbars-vertical;
        overflow-y: scroll;
      }

      *,
      *:before,
      *:after
      {
        box-sizing: inherit;
      }

      body
      {
        margin:0;
        background: #fafafa;
      }
    </style>
</head>

<body>
<div id="swagger-ui"></div>

<script src="${pageContext.request.contextPath}/webjars/swagger-ui/3.22.1/swagger-ui-bundle.js"> </script>
<script src="${pageContext.request.contextPath}/webjars/swagger-ui/3.22.1/swagger-ui-standalone-preset.js"> </script>
<script>
    const DisableTryItOutPlugin = function() {
      return {
        statePlugins: {
          spec: {
            wrapSelectors: {
              allowTryItOutFor: () => () => false
            }
          }
        }
      }
    }

    window.onload = function () {
      window.ui = SwaggerUIBundle({
        url: "${pageContext.request.contextPath}/rest/swagger.json",
        dom_id: '#swagger-ui',
        deepLinking: true,
        presets: [
          SwaggerUIBundle.presets.apis
        ],
        plugins: [
          //DisableTryItOutPlugin,
          SwaggerUIBundle.plugins.DownloadUrl
        ]
      });
    };
</script>

</body>
</html>