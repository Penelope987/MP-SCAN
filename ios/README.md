# MP SCAN para iPhone — 0.1.0

Primeira etapa nativa em SwiftUI, baseada no contrato de dados do Android 6.3.2. **Não é um IPA assinado nem uma versão pronta para divulgar ao público.**

## Implementado nesta etapa

- Projeto para iPhone e iPad, iOS 17 ou posterior; navegação adaptativa e temas claro/escuro/sistema.
- Configuração iOS do mesmo servidor do Android, sem renomear MP SCAN.
- Login por e-mail/senha e Google, cadastro, recuperação de senha e preenchimento obrigatório de nome/@.
- Catálogo com atualização em tempo real, detalhes, temporadas recolhíveis, agendamento e NOVO de 48 horas (editar não reinicia o prazo).
- Busca de obras por título/autor/sinopse; usuários por nome/@ com avatar e abertura do perfil público. Não lê os dados privados de terceiros.
- Leitor nativo de imagens, novels, HTML sanitizado e texto+imagens na ordem original; não executa scripts HTML.
- Download de todos os blocos do capítulo, conferência de imagens, tamanho e SHA-256; publicação local depois da validação. Falha preserva a cópia anterior.
- Biblioteca offline separada por conta. Logout troca o acesso; não apaga os arquivos baixados daquela conta.
- Verificação de banimento, cache por usuário e bloqueio de contas suspensas conhecidas.
- Progresso de leitura local: 100% somente depois do final e de todos os blocos carregados.
- Mensagens de erro em português, tamanho de texto e cores de destaque.

## Ainda não adaptado — não anunciar como pronto

- Painel administrativo, comentários/respostas, notificações push, seguir/solicitações e moderação.
- Favoritos/coleções/histórico sincronizados; o progresso desta etapa é local no iPhone.
- Hospedagens, importação de parceiros externos e seus catálogos.
- Molduras, selos customizados, plano de fundo personalizado e todos os controles de aparência do Android.
- Verificação etária externa: obras identificadas como restritas são bloqueadas até integrar o fluxo. Conferir os campos do catálogo real.
- Downloads em segundo plano persistentes e baixar todos por temporada. Nesta etapa mantenha o aplicativo aberto para baixar; cancelamento/fechamento não cria um download falsamente concluído.
- Autoscroll e restauração exata da posição do leitor.
- Políticas iOS definitivas e revisão de privacidade antes da distribuição pública.

## Abrir e compilar no Mac

1. Entre na pasta `ios` do pacote. Use Xcode 16 ou posterior e suas ferramentas de linha de comando.
2. Instale XcodeGen: `brew install xcodegen`.
3. O pacote entregue contém seu `App/GoogleService-Info.plist`. No GitHub ele fica ignorado: copie seu arquivo para esse caminho.
4. Execute `python3 scripts/generate.py` nesta pasta.
5. Abra `MPSCAN.xcodeproj`. Selecione o target MPSCAN, Signing & Capabilities, sua equipe Apple.
6. Mantenha **Bundle Identifier: com.mpscan**, exatamente como registrado. Não use o identificador Android.
7. Rode no simulador e em um iPhone real. A configuração de exemplo só compila/testa a interface; não autentica no servidor real.

## Login Google

O gerador adiciona o esquema de URL `REVERSED_CLIENT_ID` do seu plist. Use o mesmo projeto e habilite o provedor Google no Authentication. O SDK recebe o `CLIENT_ID` iOS e troca os tokens pela credencial de autenticação. **No iOS não se cadastram SHA-1/SHA-256 do APK**: esses certificados são do Android.

## Testes

`cd Core && swift test` executa os testes de modelos, conteúdo misto, novels, ordem das páginas, expiração de NOVO, bans, integridade offline e caminhos de arquivos.

O workflow `.github/workflows/ios.yml` (na raiz do repositório) gera o projeto, compila e executa testes da navegação em simulador. Esses testes usam conta/catálogo fictícios. **Eles não comprovam login Google real, permissões de produção nem downloads reais.**

Teste manual obrigatório: login e cancelamento Google, perfil incompleto, banimento online/offline, novel e capítulo misto, falha no meio do download, abrir em modo avião após reiniciar, contas distintas, armazenamento cheio, pouca memória, retorno da internet e iPad em paisagem.

## Instalar sem publicar na App Store

Para distribuir via TestFlight é necessária uma assinatura Apple Developer Program, configuração de assinatura e envio pelo App Store Connect. A primeira versão para público externo passa pela análise Apple; cada build tem validade de até 90 dias. A assinatura da Apple e as credenciais privadas não estão neste pacote.

Depois dos testes de dispositivo: Product > Archive > Distribute App > App Store Connect. Cadastre o aplicativo com `com.mpscan`, complete privacidade/conformidade, envie a versão e obtenha o link público do TestFlight. Até isso ocorrer, **não existe link de download iOS para colocar no site**.

Não troque suas regras de banco apenas para esta etapa: o código usa os caminhos já existentes; valide as permissões na conta real sem afrouxá-las.

## Verificação executada nesta entrega

- 19 testes XCTest do módulo MPSCANCore executados em Linux com Swift 6.0.3: zero falhas.
- Análise sintática de todos os arquivos Swift da interface e dos testes UI: aprovada. Isto não é compilação contra os frameworks iOS.
- Plist conferido: mesmo projeto do Android e Bundle ID `com.mpscan`; esquema de retorno Google derivado do arquivo.
- Compilação Xcode, testes UI de simulador, login e downloads no servidor real e testes em aparelhos: **pendentes**.
- A revisão automática bloqueou a publicação da árvore de código no GitHub por falta de autorização explícita para envio ao repositório público. Nenhum commit do projeto iOS nem teste Actions foi executado. Foi criada apenas a branch vazia e um objeto do ícone.

Documentação usada: [SDK Firebase iOS](https://github.com/firebase/firebase-ios-sdk), [login Google iOS](https://firebase.google.com/docs/auth/ios/google-signin), [dados em tempo real](https://firebase.google.com/docs/database/ios/read-and-write), [TestFlight](https://developer.apple.com/testflight/), [cadastro Apple](https://developer.apple.com/programs/enroll/).
