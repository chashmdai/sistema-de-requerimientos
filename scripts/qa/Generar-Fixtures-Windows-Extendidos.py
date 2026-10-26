"""Solo documentos sintéticos. Ejecutar tras Generar-Fixtures-Documentales.py."""
from pathlib import Path
import sys,zipfile,wave,struct
p=Path(sys.argv[1]).resolve()
assert p.parent.name.startswith('documental-win-') and (p/'oficina.docx').is_file()
def zipdoc(name,items):
 assert not (p/name).exists()
 with zipfile.ZipFile(p/name,'w',zipfile.ZIP_DEFLATED) as z:
  for key,value in items.items():z.writestr(key,value)
with zipfile.ZipFile(p/'oficina.docx') as z: good={n:z.read(n) for n in z.namelist()}
for name,change in {
 'xxe.docx':{'word/document.xml':b'<!DOCTYPE x [<!ENTITY e SYSTEM "file:///C:/req/config/NO_LEER">]><x>&e;</x>'},
 'traversal.docx':{'../escape.txt':b'SINTETICO'},
 'macro.docx':{'word/vbaProject.bin':b'NO_EJECUTABLE_SINTETICO'},
 'formula.docx':{'word/document.xml':b'<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:instrText>DDE external</w:instrText></w:document>'}
}.items():zipdoc(name,good|change)
name=p/'symlink.docx';assert not name.exists()
with zipfile.ZipFile(name,'w',zipfile.ZIP_DEFLATED) as z:
 for n,b in good.items():z.writestr(n,b)
 e=zipfile.ZipInfo('link');e.create_system=3;e.external_attr=0o120777<<16;z.writestr(e,'C:/req/config/NO_LEER')
for ext,folder,root,namespace,content in [
 ('xlsx','xl/workbook.xml','workbook','http://schemas.openxmlformats.org/spreadsheetml/2006/main','application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml'),
 ('pptx','ppt/presentation.xml','presentation','http://schemas.openxmlformats.org/presentationml/2006/main','application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml')]:
 zipdoc('oficina.'+ext,{'[Content_Types].xml':f'<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Override PartName="/{folder}" ContentType="{content}"/></Types>', '_rels/.rels':f'<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="r1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="{folder}"/></Relationships>',folder:f'<{root} xmlns="{namespace}"/>'})
assert not (p/'oficina.odt').exists()
with zipfile.ZipFile(p/'oficina.odt','w') as z:
 z.writestr('mimetype','application/vnd.oasis.opendocument.text',compress_type=zipfile.ZIP_STORED)
 z.writestr('content.xml','<office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"/>')
 z.writestr('META-INF/manifest.xml','<manifest:manifest xmlns:manifest="urn:oasis:names:tc:opendocument:xmlns:manifest:1.0"/>')
(p/'texto.txt').write_text('SIGER QA completamente sintético',encoding='utf-8')
(p/'binario.txt').write_bytes(b'\x00\xff\x01')
(p/'roto.pdf').write_bytes(b'%PDF-1.7\ninvalid')
print('Fixtures Office/XXE/ZIP/TXT preparados sin datos reales')
