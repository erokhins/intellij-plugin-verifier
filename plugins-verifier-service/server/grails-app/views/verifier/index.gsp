<!doctype html>
<html>
<head>
    <title>Welcome to Grails</title>
</head>

<body>

Check plugin range: <br/>
<g:uploadForm action="checkPluginRange" method="post">
    <input type="file" name="pluginFile"/>
    <input type="submit" value="Check plugin"/>
</g:uploadForm>



Check IDE against all compatible plugins: <br/>
<g:uploadForm action="checkIdeWithAllCompatibleUpdates" method="post">
    <input type="file" name="ideFile"/>
    <input type="submit" value="Check ide"/>
</g:uploadForm>

</body>
</html>
