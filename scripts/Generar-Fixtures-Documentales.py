from pathlib import Path
import argparse,struct,zlib,zipfile,io
parser=argparse.ArgumentParser(description='Generar documentos de prueba exclusivamente sintéticos');parser.add_argument('out',type=Path);args=parser.parse_args()
out=args.out.resolve()
if out.exists() and any(out.iterdir()):parser.error('Usar un directorio nuevo o vacío; no se sobrescriben archivos existentes')
out.mkdir(parents=True,exist_ok=True)
def pdf(active=False):
 objects=[b'<< /Type /Catalog /Pages 2 0 R'+(b' /OpenAction << /S /JavaScript /JS (app.alert) >>' if active else b'')+b' >>',b'<< /Type /Pages /Count 3 /Kids [3 0 R 5 0 R 7 0 R] >>']
 for i,color in enumerate(['0.9 0.95 1','1 0.9 0.9','0.9 1 0.9']):
  objects.append(b'<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 9 0 R >> >> /Contents '+str(4+2*i).encode()+b' 0 R >>')
  content=f'q {color} rg 0 0 595 842 re f Q BT /F1 28 Tf 50 720 Td (SIGER SINTETICO PAGINA {i+1}) Tj ET'.encode()
  objects.append(b'<< /Length '+str(len(content)).encode()+b' >>\nstream\n'+content+b'\nendstream')
 objects.append(b'<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>')
 result=b'%PDF-1.4\n';offsets=[0]
 for i,obj in enumerate(objects,1):offsets.append(len(result));result+=f'{i} 0 obj\n'.encode()+obj+b'\nendobj\n'
 xref=len(result);result+=f'xref\n0 {len(objects)+1}\n0000000000 65535 f \n'.encode()+b''.join(f'{o:010d} 00000 n \n'.encode() for o in offsets[1:]);result+=f'trailer\n<< /Size {len(objects)+1} /Root 1 0 R >>\nstartxref\n{xref}\n%%EOF\n'.encode();return result
(out/'tres-paginas.pdf').write_bytes(pdf());(out/'activo.pdf').write_bytes(pdf(True));(out/'falso.png').write_text('No es una imagen')
(out/'control-antivirus.txt').write_bytes(b'X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*')
def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)
png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',32,32,8,2,0,0,0))+chunk(b'IDAT',zlib.compress((b'\0'+b'\x30\x70\xb0'*32)*32))+chunk(b'IEND',b'')
(out/'imagen.png').write_bytes(png)
types='<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>'
word='<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body><w:p><w:r><w:t>SINTETICO</w:t></w:r></w:p></w:body></w:document>'
for name,external in [('oficina.docx',False),('enlace-falso.docx',True)]:
 target='https://example.invalid/control' if external else 'word/document.xml'
 rels='<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="r1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="'+target+'"'+(' TargetMode="External"' if external else '')+'/></Relationships>'
 with zipfile.ZipFile(out/name,'w',zipfile.ZIP_DEFLATED) as archive:
  archive.writestr('[Content_Types].xml',types);archive.writestr('_rels/.rels',rels);archive.writestr('word/document.xml',word)
print('Fixtures sintéticos preparados. El archivo de control antivirus es exclusivamente una prueba de laboratorio.')