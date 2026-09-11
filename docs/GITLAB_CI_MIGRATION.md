# GitLab CI como motor externo do GuitarLab

O GitHub permanece como origem oficial do código. O GitLab executa o pipeline definido em
`.gitlab-ci.yml`. Para não consumir minutos compartilhados, use um GitLab Runner próprio com
Linux, virtualização/KVM e a tag `android-kvm`.

## Opção A — integração nativa (GitLab Premium/Ultimate)

1. No GitLab, selecione **Create new > New project/repository**.
2. Selecione **Run CI/CD for external repository > GitHub**.
3. Autorize o GitHub e selecione o repositório privado `anfalcir/guitarlab`.
4. Confirme que o projeto foi criado como repositório externo e que o pull mirror está ativo.
5. Em **Settings > Repository > Mirroring repositories**, habilite **Trigger pipelines for mirror
   updates** quando a opção estiver disponível.
6. Não faça pushes no espelho GitLab. Todo push continua sendo feito no GitHub.

Essa integração registra webhooks de `push` e `pull_request`. Um push em branch com PR aberto pode
gerar dois pipelines; as regras deste projeto aceitam o pipeline externo e as branches `main` e
`dev/parallel-m3-m5`.

## Opção B — GitLab Free com webhook

A integração nativa para repositórios externos e pull mirroring é Premium/Ultimate. No plano Free,
use um projeto GitLab privado mínimo apenas para orquestração:

1. Crie um projeto vazio privado no GitLab, por exemplo `guitarlab-ci`.
2. Copie para ele apenas o `.gitlab-ci.yml` deste repositório e mantenha sua branch padrão como
   `main`. O código do app não é hospedado nesse projeto.
3. Em **Settings > CI/CD > Pipeline trigger tokens**, crie um token chamado `github-push`.
4. Anote o **Project ID** exibido na página inicial do projeto.
5. No GitHub, abra `anfalcir/guitarlab` e acesse **Settings > Webhooks > Add webhook**.
6. Use como Payload URL:

   `https://gitlab.com/api/v4/projects/PROJECT_ID/ref/main/trigger/pipeline?token=TRIGGER_TOKEN`

7. Selecione `application/json`, mantenha SSL verification habilitado e marque somente **Pushes**.
8. Faça um push de teste. O GitLab recebe o payload, confere que o repositório é exatamente
   `anfalcir/guitarlab`, clona o SHA `after` e confirma o hash antes do build.

## Runner próprio sem cota compartilhada

1. No projeto GitLab, acesse **Settings > CI/CD > Runners > New project runner**.
2. Selecione Linux, informe a tag `android-kvm`, desative **Run untagged jobs** e crie o runner.
3. Instale e registre o GitLab Runner no computador Linux seguindo o comando exibido pelo GitLab.
4. Use o executor `shell`. O usuário do runner precisa acessar `/dev/kvm` (normalmente adicionando-o
   ao grupo `kvm` e reiniciando o serviço).
5. Instale no host: Git, Python 3, unzip, JDK 17, Android command-line tools, emulator, adb e Gradle
   9.6.1. Defina `ANDROID_HOME` ou `ANDROID_SDK_ROOT` no ambiente do serviço.
6. Aceite as licenças do Android SDK e confirme:

   - `java -version` aponta para Java 17;
   - `gradle --version` informa 9.6.1;
   - `test -r /dev/kvm && test -w /dev/kvm` passa para o usuário do runner;
   - `sdkmanager`, `avdmanager`, `emulator` e `adb` estão no `PATH`.

O runner próprio não consome minutos dos runners hospedados. O computador precisa estar ligado e o
serviço `gitlab-runner` ativo quando o webhook disparar.

## Variáveis e secrets

No GitLab, abra **Settings > CI/CD > Variables**:

| Chave | Tipo | Visibilidade | Uso |
|---|---|---|---|
| `GH_READ_TOKEN` | Variable | Masked and hidden | Token GitHub somente leitura para clonar o repositório privado no modo Free/webhook |
| `GUITARLAB_SIGNING_BUNDLE_B64` | Variable | Masked and hidden | Mesmo bundle base64 usado na assinatura oficial |
| `SIGNED_HOMOLOGATION` | Variable | Visible | Defina manualmente como `true` somente para gerar candidato assinado |

Marque o secret de assinatura como **Protected** e proteja as branches que podem gerar release. No
modo de repositório externo Premium, `GH_READ_TOKEN` não é necessário porque o GitLab já fornece o
checkout do espelho.

O token GitHub deve ter apenas acesso de leitura ao conteúdo de `anfalcir/guitarlab`. Não use token
com permissão de escrita ou administração.

## Corte definitivo do GitHub Actions

Só remova o gatilho `push` de `.github/workflows/android-ci.yml` depois que um pipeline GitLab tiver
passado nos três gates. Até esse aceite, o workflow GitHub permanece como fallback manual e como
referência auditável.
