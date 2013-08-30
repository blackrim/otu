#!/usr/bin/python

import json
import urllib2
import cgi,cgitb
from StringIO import StringIO
cgitb.enable()

import os, sys
try: # Windows needs stdio set for binary mode.
    import msvcrt
    msvcrt.setmode (0, os.O_BINARY) # stdin  = 0
    msvcrt.setmode (1, os.O_BINARY) # stdout = 1
except ImportError:
    pass

#UPLOAD_DIR = "/tmp"

HTML_TEMPLATE = """<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
    <title>OTU: get data in there</title>
    <link href="../css/bootstrap.min.css" rel="stylesheet" media="screen">
    <link href="../css/bootstrap-responsive.css" rel="stylesheet">
    <script src="../js/jquery-1.9.1.js"></script>
    <script src="../js/bootstrap.min.js"></script>
    <script src="../js/jquery-ui-1.10.3.custom.min.js"></script>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
      body {
        padding-top: 60px; /* 60px to make the container go all the way to the bottom of the topbar */
      }
    </style>
  </head>
  <body>
    <div class="navbar navbar-inverse navbar-fixed-top">
      <div class="navbar-inner">
        <div class="container">
          <button type="button" class="btn btn-navbar" data-toggle="collapse" data-target=".nav-collapse">
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
          </button>
          <a class="brand" href="#">OTU</a>
          <div class="nav-collapse collapse">
            <ul class="nav">
              <li><a href="load_studies.py"">Load and List Studies</a></li>
              <li class="active"><a href="#">Search Public Studies</a></li>
              <li><a href="../study_view.html">View Study</a></li>
              <li><a href="../tree_dyn.html">View Tree</a></li>
            </ul>
          </div><!--/.nav-collapse -->
        </div>
      </div>
    </div>
    <div class="container">
      <h1>Search remote studies</h1>
      <div class="row">
        <div id="study-contain" class="span5">
          <form action="search_studies.py" method="POST" enctype="multipart/form-data">
            <input type="hidden" id="init_remote_indexing_flag" name="init_remote_indexing_flag"/>
            <button name="submit" type="submit" class="btn">Build indexes</button>
          </form>
        </div>
      </div>
    </div>
  </body>
</html>
"""

remote_study_indexing_service_url = "http://localhost:7474/db/data/ext/studyJsons/graphdb/indexRemoteStudies"

def init_build_remote_study_indexes():
    data = None

    # example of accessing form elements from previous form submission
    form = cgi.FieldStorage()
    if form.has_key("init_remote_indexing_flag"):

        ### here is an example of how to call a neo4j rest service
        req = urllib2.Request(remote_study_indexing_service_url, headers = {"Content-Type": "application/json",
            # Some extra headers for fun
            "Accept": "*/*",   # curl does this
            "User-Agent": "my-python-app/1", # otherwise it uses "Python-urllib/..."
            },data = data)
        f = urllib2.urlopen(req)

def print_html_form():
    """This prints out the html form. Note that the action is set to
      the name of the script which makes this is a self-posting form.
      In other words, this cgi both displays a form and processes it.
    """
    print "content-type: text/html\n"

    print HTML_TEMPLATE

    # here is an example of how to print the rest results to the page. see below for how to get the rest results
#    print HTML_TEMPLATE.replace("GITFILELIST",gitfilelist)

################################# old stuff from load_studies.py for reference below here

#resturlsinglenewick = "http://localhost:7474/db/data/ext/studyJsons/graphdb/putStudyNewickSingle"
#resturlnexsonfile = "http://localhost:7474/db/data/ext/studyJsons/graphdb/putStudyNexsonFile"
#
#def make_json_with_newick(studyname,newick):
#    data = json.dumps({"studyID": studyname, "newickString": newick})
#    return data
#
#def make_json_with_nexson(studyname,nexson):
#    data = json.dumps({"studyID": studyname, "nexsonString": nexson})
#    return data
#    
## replaced with new version above
##def print_html_form (success,gitfilelist):
##    """This prints out the html form. Note that the action is set to
##      the name of the script which makes this is a self-posting form.
##      In other words, this cgi both displays a form and processes it.
##    """
##    print "content-type: text/html\n"
##    print HTML_TEMPLATE.replace("GITFILELIST",gitfilelist);
#
#def save_uploaded_file(form_field, upload_dir):
#
#    # example of accessing form elements from previous form submission
#    form = cgi.FieldStorage()
#    log = open("logging","w")
#    log.write("HERE")
#    for i in form.keys():
#    log.write(i)
#    log.close()
#    if form.has_key("hidden_newick_from_file"):
#        save_uploaded_file_newick(form, form_field,upload_dir)
#    elif form.has_key("hidden_nexson_from_git"):
#        save_git_file_nexson(form, upload_dir)
#    elif form.has_key("hidden_nexson_from_file"):
#        save_uploaded_file_nexson(form, form_field,upload_dir)
#    else:
#        return False
#
#def get_bitbucket_file_list():
#    commiturl = "https://bitbucket.org/api/2.0/repositories/blackrim/avatol_nexsons/commits"
#    req = urllib2.Request(commiturl)
#    f = urllib2.urlopen(req)
#    cjson = json.loads(f.read())
#    recenthash = cjson["values"][0]["hash"]
#
#    listurl = "https://bitbucket.org/api/1.0/repositories/blackrim/avatol_nexsons/raw/"+recenthash+"/"
#    req = urllib2.Request(listurl)
#    f = urllib2.urlopen(req)
#    files = []
#    for i in f:
#    try:
#        int(i.strip())
#        files.append(i.strip())
#    except:
#        continue
#    retstr = ""
#    for i in files:
#    retstr += "<option value="+recenthash+"/"+i+">"+i+"</option>\n"
#    return retstr
#        
#def save_uploaded_file_newick (form, form_field, upload_dir):
#    log = open("logging","a")
#    """This saves a file uploaded by an HTML form.
#       The form_field is the name of the file input field from the form.
#       For example, the following form_field would be "file_1":
#           <input name="file" type="file">
#       The upload_dir is the directory where the file will be written.
#       If no file was uploaded or if the field does not exist then
#       this does nothing.
#    """
#    if not form.has_key(form_field): return False
#    studyid = form["studyID"].value
#    fileitem = form[form_field]
#    if not fileitem.file: return False
#    fout = file (os.path.join(upload_dir, fileitem.filename), 'wb')
#    while 1:
#        chunk = fileitem.file.read(100000)
#        if not chunk: break
#        fout.write (chunk)
#    fout.close()
#    fout = open (os.path.join(upload_dir, fileitem.filename), 'r')
#    data = ""
#    for i in fout:
#        data = make_json_with_newick(studyid,i.strip())
#        log.write( data+"\n")
#        break
#    fout.close()
#    
#    
#    ### here is an example of how to call a neo4j rest service
#    req = urllib2.Request(resturlsinglenewick, headers = {"Content-Type": "application/json",
#        # Some extra headers for fun
#        "Accept": "*/*",   # curl does this
#        "User-Agent": "my-python-app/1", # otherwise it uses "Python-urllib/..."
#        },data = data)
#    f = urllib2.urlopen(req)
#
#
#    log.close()
#    return True
#
#def save_git_file_nexson(form, upload_dir):
#    log = open("logging","a")
#    studyID = form["loadselect"].value
#    geturl = "https://bitbucket.org/api/1.0/repositories/blackrim/avatol_nexsons/raw/"+str(studyID)
#    req = urllib2.Request(geturl)
#    f = urllib2.urlopen(req)
#    nexson = json.dumps(json.loads(f.read()))
#    data = make_json_with_nexson(studyID,nexson)
#    clen = len(data)
#    req2 = urllib2.Request(resturlnexsonfile, headers = {"Content-Type": "application/json",
#    "Content-Length": clen,
#        # Some extra headers for fun
#        "Accept": "*/*",   # curl does this
#        "User-Agent": "my-python-app/1", # otherwise it uses "Python-urllib/..."
#        },data = data)
#    log.write('curl -X POST '+resturlnexsonfile+' -H "Content-Type: application/json"'+' -d \''+data+'\'')
#    f = urllib2.urlopen(req2)
#    log.close()
#    return True
#
#def save_uploaded_file_nexson (form, form_field, upload_dir):
#    return False

#success = save_uploaded_file ("file", UPLOAD_DIR)
#gitfilelist = get_bitbucket_file_list()
#print_html_form (success,gitfilelist)

init_build_remote_study_indexes()

# print the html to the web browser which will render it
print_html_form()
