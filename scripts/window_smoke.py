#!/usr/bin/env python3
"""Recents/window smoke test: select Andi's floating control, then exercise task resizing."""
import pathlib,re,subprocess,sys,xml.etree.ElementTree as E
ROOT=pathlib.Path(__file__).resolve().parents[1];ADB=str(ROOT/'.tools/android-sdk/platform-tools/adb')
compact=False
def a(*args):return subprocess.run([ADB,*args],check=True,capture_output=True).stdout
def snapshot():
 a('shell','uiautomator','dump','/sdcard/andi-ui.xml')
 return E.fromstring(a('shell','cat','/sdcard/andi-ui.xml'))
def tap(node):
 x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds')))
 x,y=(x1+x2)//2,(y1+y2)//2
 if compact and node.get('package')=='com.andi.proxy':x,y=scaled(x,y)
 a('shell','input','tap',str(x),str(y))
def scaled(x,y):
 # The test ROM renders the observed freeform surface at 0.8 scale,
 # while accessibility bounds remain in the task's unscaled coordinates.
 return round(200+(x-200)*.8),round(400+(y-400)*.8)
def scroll_up(node):
 x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds')))
 x,y=scaled((x1+x2)//2,y2-20);ex,ey=scaled((x1+x2)//2,y1+20)
 a('shell','input','swipe',str(x),str(y),str(ex),str(ey),'400')
def require_compact():
 lines=a('shell','dumpsys','activity','activities').decode().splitlines()
 for i,line in enumerate(lines):
  if '* Task{' in line and 'com.andi.proxy' in line and 'mode=' in line:
   if 'mode=freeform' not in line or 'Rect(200, 400 - 970, 1240)' not in lines[i+1]:
    raise RuntimeError('Compact window was not retained; compact layout not verified')
   return
 raise RuntimeError('Andi compact task not found')
try:
 a('shell','am','start','--windowingMode','1','-n','com.andi.proxy/.MainActivity');a('shell','input','keyevent','187')
 clicked=False
 for attempt in range(4):
  a('shell','uiautomator','dump','/sdcard/andi-ui.xml');r=E.fromstring(a('shell','cat','/sdcard/andi-ui.xml'));parent={c:p for p in r.iter() for c in p}
  header=next((n for n in r.iter('node') if n.get('resource-id','').endswith('header_icon') and n.get('content-desc')=='Andi Proxy'),None)
  if header is None:break
  p=header;found=None
  for i in range(5):
   p=parent.get(p,p);hs=[n for n in p.iter('node') if n.get('resource-id','').endswith('header_icon')]
   if len(hs)>1:break
   fs=[n for n in p.iter('node') if n.get('resource-id','').endswith('splitscreen_task')]
   if fs:found=fs[0];break
  if found is not None:
   x1,y1,x2,y2=map(int,re.findall(r'\d+',found.get('bounds')));a('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));clicked=True;break
  a('shell','input','swipe','1080','1400','430','1400','400')
 if not clicked:raise RuntimeError('Andi floating control not found')
 a('shell','uiautomator','dump','/sdcard/andi-ui.xml')
 before=a('shell','dumpsys','activity','activities').decode()
 task=next((line for line in before.splitlines() if 'Task{' in line and 'com.andi.proxy' in line and 'mode=freeform' in line),None)
 if task is None:raise RuntimeError('Andi did not enter freeform mode')
 task_id=re.search(r'#(\d+)',task).group(1);print('PASS Andi native floating window')
 a('shell','am','task','resize',task_id,'180','320','1100','2200')
 a('shell','uiautomator','dump','/sdcard/andi-ui.xml')
 after=a('shell','dumpsys','activity','activities').decode();lines=after.splitlines()
 resized=False
 for i,l in enumerate(lines):
  if 'Task{' in l and 'com.andi.proxy' in l and 'mode=freeform' in l:
   print(l.strip()[:190]);print(lines[i+1].strip()[:160]);resized='Rect(180, 320 - 1100, 2200)' in lines[i+1];break
 if not resized:raise RuntimeError('Requested window bounds not applied')
 print('PASS window resize')
 if '--compact' in sys.argv:
  a('shell','am','task','resize',task_id,'200','400','970','1240')
  snapshot();require_compact()
  (ROOT/'.tools/runtime/min-before-focus.png').write_bytes(a('exec-out','screencap','-p'))
  compact=True
  a('shell','input','tap','230','650')
  r=snapshot();require_compact();settings=next((n for n in r.iter('node') if n.get('text')=='首页'),None)
  if settings is None:raise RuntimeError('Settings navigation unavailable in compact window')
  tap(settings)
  picker=None
  for _ in range(5):
   r=snapshot();picker=next((n for n in r.iter('node') if n.get('text','').startswith('不代理的应用')),None)
   if picker is not None:break
   scroll=next((n for n in r.iter('node') if n.get('class')=='android.widget.ScrollView'),None)
   if scroll is None:raise RuntimeError('Compact settings viewport unavailable')
   scroll_up(scroll)
  if picker is None:raise RuntimeError('Whitelist entry unavailable in compact window')
  tap(picker);r=snapshot();require_compact()
  save=next((n for n in r.iter('node') if n.get('text')=='保存白名单'),None)
  listing=next((n for n in r.iter('node') if n.get('class')=='android.widget.ListView'),None)
  if save is None or listing is None:raise RuntimeError('Whitelist controls unavailable in compact window')
  x1,y1,x2,y2=map(int,re.findall(r'\d+',listing.get('bounds')))
  if y2-y1<40:raise RuntimeError('Whitelist list has insufficient visible height')
  for _ in range(5):
   if any(n.get('class')=='android.widget.CheckBox' for n in r.iter('node')):break
   scroll_up(listing);r=snapshot()
  else:raise RuntimeError('App checkboxes unavailable after compact list scrolling')
  print('PASS compact whitelist list',listing.get('bounds'),'save',save.get('bounds'))
  a('shell','input','keyevent','4')
except Exception as e:
 print('WINDOW TEST LIMIT:',e)
 (ROOT/'.tools/runtime/window-limit.png').write_bytes(a('exec-out','screencap','-p'))
 raise SystemExit(1)
finally:
 s=a('shell','dumpsys','activity','activities').decode()
 if any('ResumedActivity' in line and '/.AppPickerActivity' in line for line in s.splitlines()):a('shell','input','keyevent','4')
 a('shell','am','start','--windowingMode','1','-n','com.andi.proxy/.MainActivity')
