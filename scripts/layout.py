import json
from collections import Counter
# slots: [tap, left, up, right, down]
L1 = [
 ("Pronouns",        ["ni","ona","sina","jan","mi"]),
 ("Grammar",         ["li","la","o","e","pi"]),
 ("Prepositions",    ["lon","tan","sama","tawa","kepeken"]),
 ("Negation & logic",["ala","anu","seme","taso","en"]),
 ("Language",        ["toki","nimi","sitelen","lipu","musi"]),
 ("Mind & will",     ["wile","ken","kama","sona","pilin"]),
 ("Actions",         ["pali","pana","jo","lukin","ante"]),
 ("Qualities",       ["wawa","lili","pona","suli","ike"]),
 ("Numbers",         ["mute","wan","tu","ale","nanpa"]),
 ("Time & place",    ["tenpo","ma","tomo","nasin",""]),
 ("Things & life",   ["ijo","ilo","moku","lape",""]),
 ("Interjections",   ["a","n","mu","kin",""]),
]
L2 = [
 ("Animals",         ["soweli","kala","waso","pipi","akesi"]),
 ("Plants & food",   ["kasi","kili","pan","soko","kijetesantakalu"]),
 ("Sky & light",     ["suno","mun","sewi","pimeja",""]),
 ("Matter",          ["telo","ko","kon","kiwen",""]),
 ("Colors",          ["kule","loje","jelo","laso","walo"]),
 ("Body",            ["sijelo","uta","lawa","luka","noka"]),
 ("People",          ["kulupu","mama","meli","mije","tonsi"]),
 ("Feelings",        ["olin","suwi","monsuta","jaki","nasa"]),
 ("Surfaces & holes",["selo","","nena","","lupa"]),
 ("Sound",           ["kalama","kute","","",""]),
 ("Life & death",    ["unpa","moli","misikeke","",""]),
 ("Heat",            ["seli","","","","lete"]),
]
L3 = [
 ("Position",        ["insa","monsi","poka","sinpin","anpa"]),
 ("Box, open & close",["poki","open","awen","pini","weka"]),
 ("Conflict",        ["utala","alasa","pakala","kipisi",""]),
 ("Objects",         ["len","palisa","","supa",""]),
 ("Shapes",          ["sike","leko","linja","",""]),
 ("Money",           ["mani","esun","","",""]),
 ("New & extra",     ["sin","namako","","",""]),
 ("Books",           ["pu","ku","","",""]),
 ("",                ["","","","",""]),
 ("",                ["","","","",""]),
 ("",                ["","","","",""]),
 ("",                ["","","","",""]),
]
d=json.load(open('words.json')); by={r['word']:r for r in d}
layers=[L1,L2,L3]
used=[w for L in layers for _,ws in L for w in ws if w]
print("dup",[w for w,c in Counter(used).items() if c>1],"missing",[w for w in by if w not in used],"unknown",[w for w in used if w not in by],len(used))
json.dump([[{"g":g,"w":ws} for g,ws in L] for L in layers],open('layout.json','w'),ensure_ascii=False)
