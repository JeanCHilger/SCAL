




import os
import sys
#from sklearn.neighbors import LSHForest, NearestNeighbors 
from sklearn.feature_extraction.text import TfidfVectorizer
import numpy as np


from rank_bm25 import BM25Okapi

def loadinput(inputfile,topic):
    
    svmfil = []
    with open("data/"+topic+".pids", "r") as mapping_file:
        for line in mapping_file.readlines():
            info = line.strip().split(" ")
            svmfil.append(info[1])
            
    corpus = []
    pool=[]
    max_docs = 5000000
    cont=0
    for folder, subs, files in os.walk(inputfile.split(".")[0]):
        for filename in files:
            with open(os.path.join(folder, filename), 'r') as src:
                try:
                    txt = ' '.join(src.readlines())
                    if len(txt)> 0 and filename in svmfil :
                        corpus.append(txt)
                        pool.append(filename)
                        cont+=1                        
                except:
                    print("error exeption")
                    pass
            if len(corpus) > max_docs:
                break                
        if len(corpus) > max_docs:
            break
    print("Number of files in bm25 is ", cont)
    return corpus,pool

def loadfile(name):
    f_open = open(name, "r")
    doc=f_open.read()
    return doc

def loadgolden(name):
    f_rel = open(name, "r")
    goldendb= f_rel.readlines()
    return goldendb
    #close(f_rel)


def bm25(corpus,query,pool):
   

    tokenized_corpus = [doc.split(" ") for doc in corpus]

    bm25 = BM25Okapi(tokenized_corpus)
    
    
    tokenized_query = query.split(" ")

    doc_scores = bm25.get_scores(tokenized_query)
    sortedarray=np.argsort(doc_scores )[::-1]
  
    poolnp=np.array(pool)
  
    return (poolnp[sortedarray])


def storeranking(ranking, goldendb,fileout):
    
    
    goldendb=[l.rstrip() for l in goldendb]
    
    #print(goldendb)
    count=0
    for i in ranking:
        if i in goldendb:
            count+=1
            
        fileout.write(i+"\n")
    print ("number of relevant docs is ", count)
        
    
    

inputfile=sys.argv[1]
queryfile=sys.argv[2]
relfile=sys.argv[3]
topic=sys.argv[4]

file_out="SeedRanking"+topic
f_out = open(file_out, "w")


corpus,pool = loadinput(inputfile,topic)

query=loadfile(queryfile).lower()
goldendb=loadgolden(relfile)
print("courpos size is ", len(corpus), " input ", inputfile.split(".")[0])
ranking=bm25(corpus,query,pool)

storeranking(ranking, goldendb,f_out)
#storeout(ranking)




#import io
#import pandas as pd
#import numpy as np
#from collections import defaultdict
#from collections import Counter
#import sys 
#import pickle
#import os
#from os import path
#b=0.75
#k1=1.5


#########################3
#inputfile=sys.argv[1]
#queryfile=sys.argv[2]
#relfile=sys.argv[3]
#topic=sys.argv[4]
#############################
#def top_n(d, n):
  #dct = defaultdict(list) 
  #for k, v in d.items():
    #dct[v].append(k)      
  #return sorted(dct.items())[-n:][::-1]

#def save_obj(obj, name ):
    #with open('obj/'+ name + '.pkl'+topic, 'wb') as f:
        #pickle.dump(obj, f, pickle.HIGHEST_PROTOCOL)
    ##close('obj/'+ name + '.pkl'+topic)

#def load_obj(name ):
    #print("load .... ",'obj/' + name + '.pkl'+topic)
    #with open('obj/' + name + '.pkl'+topic, 'rb') as f:
        #return pickle.load(f)        
        
#############################


#f_seed = open(queryfile, "r")
#seed=f_seed.readlines()
##closef(f_seed)

#f_rel = open(relfile, "r")
#goldendb= f_rel.readlines()
##close(f_rel)

#file_out="SeedRanking"+topic
#f_out = open(file_out, "w")


############################################
#queryid=[]
#query=[]

##create the seed with the query string mapping to ids
#for ind in seed[0:len(seed)]:
    
    #i=ind.split(":")[0].strip()    
    #query.append(i)
#print("QUERY  STRING IS ", query, len(query))


#cnt = Counter()
#tdf = defaultdict(Counter)
#lengths = Counter()
#docs_id=set()
#count=0

#print ("Loading file ")
#vector=[]
#if (path.exists('obj/vector.pkl'+topic)):
    #vector=load_obj("vector")
    
#else: 
    ##cc 22665 98417 .1148-137204.swp 1

    ##'tok','tok_id','df','doc','tf'
    #with open(inputfile) as fileobject:
        #for line in fileobject:
            #vector.append(line.split(" "));      
            #count+=1
    
    #print(" vector len ",len(vector))
    #save_obj(vector, "vector")    
        
#print ("Loading vector tf df  ")
#for row in vector:
    #doc_id=row[3] 
    #wordid = row[1]
            
    #tdf[wordid][doc_id]+=int(row[4])
    #lengths[doc_id]=int(row[4])
    #docs_id.add(doc_id)
    #count+=1
    ##if (count%1000000==0):
        ##print("NUMBER OF TERMS PROCESSED BY BM25", count)
    #if(len(query)):
        
        #if(row[0] in query):
            #queryid.append(row[1])
            
            #query.remove(row[0])
                
#print("QUERY VECTOR ",queryid)
            
#print ("Calculing bm25 ")
#N=len(docs_id)
#soma=0
#for i in lengths.values():
  #soma+=int(i)

#avg_len = soma/N
#dff={term: len(documents) for term, documents in tdf.items()} #df
#scores = defaultdict(Counter)

##compute scores
#for doc_id in docs_id:
  #length = lengths[doc_id]
  #score = 0
  #for term in queryid:
    #tf = tdf[term][doc_id]    
    #dffvalue = dff.get(term, 0)    
    #idf = np.log((N - dffvalue + 0.5) / (dffvalue + 0.5))    
    #score += (idf * (tf * (k1 + 1)) / (tf + k1 * (1 - b + (b * length / avg_len))))
  #scores[doc_id]=score

#print ("sorting ")
#top=top_n(scores, len(scores))

#pos=[]
#count=0
#step=0
#flag=0
#print ("storing ")
#for i in top:
  #for j in i[1]:    
    #count+=1
    
    #if count<30 and any(j in s for s in goldendb):
        ##print("in the golden ", j)
        #pos.append(j)
        
    #f_out.write(j+"\n");   
        
    #if(count==30 and flag==0):
        ##print("positivos  ", pos)
        #if(len(pos)>0):
            #print("positivos  ", len(pos), " step ", step)
            #flag=1
        #step+=1
        #count=0;
        #pos=[]

#f_out.close()
