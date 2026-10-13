#!/usr/bin/env python3
import pathlib, plistlib, json, subprocess
root=pathlib.Path(__file__).resolve().parents[1]
config=root/'App/GoogleService-Info.plist'
if not config.exists():
    raise SystemExit('Coloque GoogleService-Info.plist em App antes de gerar o projeto.')
v=plistlib.loads(config.read_bytes())
if v.get('BUNDLE_ID')!='com.mpscan' or v.get('PROJECT_ID')!='meow-scan-ffff0':
    raise SystemExit('Configuração incompatível: confira o Bundle ID e o projeto selecionado.')
# JSON is a valid YAML document; avoids interpolating config values into shell commands.
project={
 'name':'MPSCAN',
 'options':{'deploymentTarget':{'iOS':'17.0'}},
 'settings':{'base':{'SWIFT_VERSION':'5.0','IPHONEOS_DEPLOYMENT_TARGET':'17.0','MARKETING_VERSION':'0.1.1','CURRENT_PROJECT_VERSION':'2','TARGETED_DEVICE_FAMILY':'1,2','ENABLE_USER_SCRIPT_SANDBOXING':'YES'}},
 'packages':{'Firebase':{'url':'https://github.com/firebase/firebase-ios-sdk.git','exactVersion':'12.4.0'}, 'GoogleSignIn':{'url':'https://github.com/google/GoogleSignIn-iOS.git','exactVersion':'8.0.0'},'MPSCANCore':{'path':'Core'}},
 'targets':{
  'MPSCAN':{'type':'application','platform':'iOS','sources':[{'path':'App','excludes':['GoogleService-Info.example.plist']}],
   'settings':{'base':{'PRODUCT_BUNDLE_IDENTIFIER':v['BUNDLE_ID'],'CODE_SIGN_STYLE':'Automatic','ASSETCATALOG_COMPILER_APPICON_NAME':'AppIcon'}},
   'info':{'path':'App/Info.plist','properties':{'CFBundleDisplayName':'MP SCAN','CFBundleURLTypes':[{'CFBundleURLSchemes':[v['REVERSED_CLIENT_ID']]}], 'UILaunchScreen':{},'UIApplicationSceneManifest':{'UIApplicationSupportsMultipleScenes':False,'UISceneConfigurations':{}},'UISupportedInterfaceOrientations':['UIInterfaceOrientationPortrait','UIInterfaceOrientationLandscapeLeft','UIInterfaceOrientationLandscapeRight'],'UISupportedInterfaceOrientations~ipad':['UIInterfaceOrientationPortrait','UIInterfaceOrientationPortraitUpsideDown','UIInterfaceOrientationLandscapeLeft','UIInterfaceOrientationLandscapeRight']}},
   'dependencies':[{'package':'Firebase','product':'FirebaseCore'},{'package':'Firebase','product':'FirebaseAuth'},{'package':'Firebase','product':'FirebaseDatabase'},{'package':'GoogleSignIn','product':'GoogleSignIn'},{'package':'MPSCANCore'}]},
  'MPSCANUITests':{'type':'bundle.ui-testing','platform':'iOS','sources':['UITests'],'dependencies':[{'target':'MPSCAN'}], 'settings':{'base':{'PRODUCT_BUNDLE_IDENTIFIER':'com.mpscan.uitests','GENERATE_INFOPLIST_FILE':'YES'}}}},
 'schemes':{'MPSCAN':{'build':{'targets':{'MPSCAN':'all'}},'test':{'targets':['MPSCANUITests'],'gatherCoverageData':True},'archive':{'config':'Release'}}}
}
(root/'project.yml').write_text(json.dumps(project,indent=2)+'\n')
subprocess.run(['xcodegen','generate','--spec',str(root/'project.yml')],cwd=root,check=True)
