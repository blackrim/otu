#!/usr/bin/python

"""
A cgi script to handle interactions with the file system required during source loading, which is
not easily done(or even possible?) with javascript. When finished with the required operations,
this script will substitute results information into the specified HTML_TEMPLATE string, and will
print the result to the web browser.
"""

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

# where we put files that we're going to work with
UPLOAD_DIR = "/tmp"

# this is the template that will be filled in and printed to the browser
HTML_TEMPLATE = open("cgi-bin/load_sources_TEMPLATE.html","rU").read()

UNSPECIFIED_ACTION = "unspecified action"
NO_ACTION = "no action"

resturlsinglenewick = "http://localhost:7474/db/data/ext/sourceJsons/graphdb/putSourceNewickSingle"
resturlnexsonfile = "http://localhost:7474/db/data/ext/sourceJsons/graphdb/putSourceNexsonFile"

# ===== functions for saving uploaded files

def save_uploaded_file(form, form_field, upload_dir):
    if (len(form.keys()) < 1):
        return {"worked":False,"message":NO_ACTION}
    log = open("logging","w")
    log.write("HERE\n")
    for i in form.keys():
        log.write(i + ": " + form[i].value + "\n")
    log.close()
    r = ""
    if form.has_key("hidden_newick_from_file"):
        r = save_uploaded_file_newick(form, form_field,upload_dir)
    elif form.has_key("hidden_nexson_from_git"):
        r = save_git_file_nexson(form, upload_dir)
    elif form.has_key("hidden_nexson_from_file"):
        r = save_uploaded_file_nexson(form, form_field,upload_dir)
    else:
        r = {"worked": False, "message":UNSPECIFIED_ACTION}
    return r
        
def save_uploaded_file_newick (form, form_field, upload_dir):
    log = open("logging","a")
    """This saves a file uploaded by an HTML form.
       The form_field is the name of the file input field from the form.
       For example, the following form_field would be "file_1":
           <input name="file" type="file">
       The upload_dir is the directory where the file will be written.
       If no file was uploaded or if the field does not exist then
       this does nothing.
    """
    if not form.has_key(form_field): return False
    sourceid = form["sourceId"].value
    fileitem = form[form_field]
    if not fileitem.file: return False
    fout = file (os.path.join(upload_dir, fileitem.filename), 'wb')
    while 1:
        chunk = fileitem.file.read(100000)
        if not chunk: break
        fout.write (chunk)
    fout.close()
    fout = open (os.path.join(upload_dir, fileitem.filename), 'r')
    data = ""
    for i in fout:
        data = make_json_with_newick(sourceid,i.strip())
        log.write( data+"\n")
        break
    fout.close()
    req = urllib2.Request(resturlsinglenewick, headers = {"Content-Type": "application/json",
        # Some extra headers for fun
        "Accept": "*/*",   # curl does this
        "User-Agent": "my-python-app/1", # otherwise it uses "Python-urllib/..."
        },data = data)
    f = urllib2.urlopen(req)
    log.close()
    return {"worked":True, "sourceId":sourceid}

def save_git_file_nexson(form, upload_dir):
    log = open("logging","a")
    recenthash = form["recenthash"].value
    sourceId = form["nexsonid"].value
    log.write("\n" + "recenthash " + recenthash + ", nexsonid " + sourceId + "\n")
    geturl = "https://bitbucket.org/api/1.0/repositories/blackrim/avatol_nexsons/raw/"+recenthash+"/"+str(sourceId)
    print geturl
    req = urllib2.Request(geturl)
    f = urllib2.urlopen(req)
    nexson = json.dumps(json.loads(f.read()))
    data = make_json_with_nexson(sourceId,nexson)
    clen = len(data)
    req2 = urllib2.Request(resturlnexsonfile, headers = {"Content-Type": "application/json",
	"Content-Length": clen,
        # Some extra headers for fun
        "Accept": "*/*",   # curl does this
        "User-Agent": "my-python-app/1", # otherwise it uses "Python-urllib/..."
        },data = data)
    log.write('curl -X POST '+resturlnexsonfile+' -H "Content-Type: application/json"'+' -d \''+data+'\'')
    f = urllib2.urlopen(req2)
    log.close()
    result = json.loads(f.read())
    result["sourceId"] = sourceId
    return result

def save_uploaded_file_nexson (form, form_field, upload_dir):
    return {"worked":False,"message":"Manual nexson upload not implemented"}

def make_json_with_newick(sourcename,newick):
    data = json.dumps({"sourceId": sourcename, "newickString": newick})
    return data

def make_json_with_nexson(sourcename,nexson):
    data = json.dumps({"sourceId": sourcename, "nexsonString": nexson})
    return data
    
# ===== functions for getting source info

def get_bitbucket_recent_hash():
    commiturl = "https://bitbucket.org/api/2.0/repositories/blackrim/avatol_nexsons/commits"
    req = urllib2.Request(commiturl)
    f = urllib2.urlopen(req)
    cjson = json.loads(f.read())
    return cjson["values"][0]["hash"]

def get_bitbucket_file_list(recenthash):
    listurl = "https://bitbucket.org/api/1.0/repositories/blackrim/avatol_nexsons/raw/"+recenthash+"/"
    req = urllib2.Request(listurl)
    f = urllib2.urlopen(req)
    files = []
    for i in f:
	try:
	    int(i.strip())
	    files.append(i.strip())
	except:
	    continue
    retstr = ""
    for i in files:
	retstr += "<option value="+i+">"+i+"</option>\n"
    return retstr

# ===== functions for making the web page

def print_html_form (result, recenthash, gitfilelist):
    """This prints out the html form. Note that the action is set to
      the name of the script which makes this is a self-posting form.
      In other words, this cgi both displays a form and processes it.
    """

    print "content-type: text/html\n"
    html = HTML_TEMPLATE.replace("$GITFILELIST$",gitfilelist).replace("$RECENTHASH$",recenthash).replace("$MESSAGE$",message)
    
    print html


# now actually do stuff

result = save_uploaded_file (cgi.FieldStorage(), "file", UPLOAD_DIR)
message = ""
#if result != UNSPECIFIED_ACTION:
if result["worked"]:
    message = '<p class="highlight">Added source '+result["sourceId"]+' to the database. <a href="../source_view.html?sourceId='+result["sourceId"]+'">Click here to edit this source</a>.</p>'
elif result["message"] != NO_ACTION:
    message = '<p class="warning">'+result["message"]+'</p>'
else:
    # there was no form submitted so don't print a warning
    pass

recenthash = get_bitbucket_recent_hash()
gitfilelist = get_bitbucket_file_list(recenthash)
print_html_form (message, recenthash, gitfilelist)
