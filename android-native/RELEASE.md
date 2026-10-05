# MP SCAN 5.0.0 — publicação

O APK distribuído deve ser compilado com `assembleRelease` e assinado fora do repositório, usando a mesma chave privada nas próximas versões. A chave e sua senha nunca devem ser incluídas no Git, nos artefatos públicos de CI ou no APK.

O backup privado é `MP-SCAN-assinatura-PRIVADA.zip`; ele contém o keystore PKCS12, o alias `mpscan-release`, a senha e o certificado público. Guarde esse backup em local privado. Ele não é um arquivo para distribuir aos leitores.

Certificado público de publicação:

- SHA-1: `A8:FD:E9:9B:B9:78:CE:5B:61:7D:84:69:73:3C:E3:29:1C:70:42:5F`
- SHA-256: `D8:3B:A6:C1:31:A0:BC:97:A2:35:A7:76:45:41:89:00:77:A0:53:DA:A2:33:12:4C:11:E6:3D:B7:12:2B:AA:70`

Cadastre esse certificado no serviço de autenticação antes de divulgar a versão com login Google. A troca da antiga assinatura de teste exige reinstalação uma vez. Atualizações futuras devem reutilizar a assinatura de publicação e um `versionCode` maior.

A CI executa testes JVM e testes no Android sem Wi-Fi/dados móveis, incluindo imagem de 28 mil pixels, renderização em regiões, substituição interrompida, arquivo truncado e restauração. O APK release não permite depuração.

Estes testes não substituem verificar o login Google e os capítulos recém-publicados em um aparelho real. Imagens removidas do servidor ou bloqueadas na origem continuam precisando de correção editorial; o aplicativo não pode reconstruir um arquivo que não existe.
