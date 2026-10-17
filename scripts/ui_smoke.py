#!/usr/bin/env python3
"""ADB UI actions use current accessibility bounds; never log credential fields."""
import pathlib,re,subprocess,sys,xml.etree.ElementTree as E
ROOT=pathlib.Path(__file__).resolve().parents[1]
ADB=str(ROOT/'.tools/android-sdk/platform-tools/adb')
def adb(*args):return subprocess.run([ADB,*args],check=True,capture_output=True).stdout
adb('shell','uiautomator','dump','/sdcard/andi-ui.xml')
xml=adb('shell','cat','/sdcard/andi-ui.xml')
nodes=list(E.fromstring(xml).iter('node'))
if len(sys.argv)>2 and sys.argv[1] in ('tap','tap-last'):
 label=sys.argv[2]
 matches=[n for n in nodes if n.get('text')==label or n.get('content-desc')==label]
 if not matches:raise SystemExit('Not visible: '+label)
 n=matches[-1] if sys.argv[1]=='tap-last' else next((n for n in matches if n.get('clickable')=='true'),matches[0])
 x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')))
 adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));print('Clicked: '+label)
elif len(sys.argv)>1 and sys.argv[1]=='buttons':
 for n in nodes:
  if n.get('clickable')=='true' and n.get('class') not in ('android.widget.EditText',):
   print(n.get('text') or n.get('content-desc'),n.get('bounds'))
elif len(sys.argv)>1 and sys.argv[1]=='status':
 for n in nodes:
  t=n.get('text','')
  if any(t.startswith(s) for s in ('未连接','已连接','尚未','已停止','正在','热点已','启动失败','代理运行','出口检测','更新成功','更新失败','本机 VPN')):
   print(t[:180])
  for state in re.findall(r'本机 VPN [^\n]*',t):print(state)
else:raise SystemExit('Usage: ui_smoke.py tap LABEL | buttons | status')
