otu
===

Here are some simple instructions to play around with this thing. 

clone with 
```
clone https://github.com/blackrim/otu.git
```

Get a neo4j server. This is very simple. Just go here http://www.neo4j.org/ and click "Download Neo4j server". Unarchive then
```
cd neo4j-community-1.9.2/
./bin/neo4j start
```

then start the otu server
```
cd otu
cd views
./server.py
```

go to http://localhost:8000/ and you are done.

Now you can upload any of the files from the avatol_nexsons in bitbucket (it pulls from there so that is 
what the list is generated from). You can type the numbers for quicker access.

Pick one, say 2539 and do submit
It will show up on the right
Click it
Bunch of the buttons don't work, but delete study from db does

You can click the tree ids to go there (delete tree from db also works)

You will see in the tree metadata that ingroup_set is set if it comes in with an ingroup or if you choose it from the treeview

Need to work on the spacing of the tips for the treeview. I have the solution but have been lazy to put it there. Maybe later tonight.

The rooting_set will only set if you reroot or choose the existing root in the tree view.

Let's add another study that doesn't have the ingroup set or rooting

Go back to the load and list studies. Add 100. 

If you go to tree 973 you will see that it doesn't have an ingroup. Click the 973. Click the choose ingroup button. Pick something. 
You will see the larger nodes and green. If you go back to teh study, you will see that the ingroup_set is true.

You can also refocus (button defaults back to that after choose_ingroup is done) and only the ingroup has the larger nodes.

Major missing things are adding more properties (including that all is well and vetted and ready to go), pushing back to git (it should do a diff with what is there so you know that things are new -- that would be easy to do),
and the names validation which I think would also be pretty doable with the TNRS. Plenty of other stuff would be fun.
