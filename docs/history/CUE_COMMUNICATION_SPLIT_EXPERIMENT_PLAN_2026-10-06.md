# CUE Communication Split — plano experimental de implementação

Updated: 2026-10-06

## Status

**EXPERIMENTAL / NÃO SUPORTADO ATÉ PROVA FÍSICA DE ROTA E FIDELIDADE**

Implementação concluída em `0.5.0-rc31` / versionCode `51`. Qualificação digital PASS em Android CI #969 / run `37549317925`, producer SHA `00fe4d621849bf5f8f1211e25ecba9d6a6c28598`. O APK assinado está pronto somente para homologação física; nenhum suporte de rota ou fidelidade é declarado antes do teste no SM-X230.

Este plano investiga se o GuitarLab consegue manter MAIN na estratégia de mídia e encaminhar CUE pela estratégia pública de comunicação do Android, reproduzindo de forma controlada o tipo de separação observado em cenários como media no Android Auto e conteúdo de comunicação no dispositivo local.

O objetivo não é imitar comportamento proprietário de outro aplicativo. O objetivo é testar, com APIs públicas, se duas estratégias distintas permitem uma topologia que dois `USAGE_MEDIA` independentes não conseguiram manter no Samsung SM-X230.

## Hipótese

Estado atual rejeitado:

```text
MAIN = USAGE_MEDIA -> MK-300
CUE  = USAGE_MEDIA -> wired headset
resultado físico: ambos -> MK-300
```

Hipótese experimental:

```text
MAIN = USAGE_MEDIA -> MK-300
CUE  = USAGE_VOICE_COMMUNICATION -> wired headset
```

A seleção de comunicação deve usar `getAvailableCommunicationDevices()`,
`setCommunicationDevice()` e `getCommunicationDevice()`. `MODE_IN_COMMUNICATION`
é um candidato separado e não deve ser aplicado de início porque altera política de routing,
volume e estado global de áudio.

## Invariantes obrigatórios

1. **Zero ducking automático entre MAIN e CUE.**
2. Ativar CUE não pode reduzir, pausar, silenciar ou elevar MAIN.
3. GuitarLab nunca solicita `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` para CUE.
4. GuitarLab não implementa ganho automático condicionado à ativação do CUE.
5. O balanceamento de conteúdo é feito pelos controles existentes do mixer.
6. Os `AudioTrack` de saída permanecem com ganho de runtime unitário após o mixer; o app não manipula volume global de sistema para fabricar o balanceamento.
7. Volume de sistema/volume group do Android pode existir como multiplicador externo, mas não é controlado automaticamente pelo GuitarLab.
8. A estratégia só pode ser habilitada se MAIN e CUE forem fisicamente distintos em `routedDevices`.
9. Separação de rota sem qualidade suficiente é falha.
10. Qualquer convergência, mudança de rota, downgrade de canais ou comportamento de volume inesperado é fail-closed para CUE.

## Fase 1 — discovery de communication devices

Adicionar um probe read-only que registre:

- `AudioManager.getAvailableCommunicationDevices()`;
- `getCommunicationDevice()`;
- tipos, produtos e identidades físicas sanitizadas;
- presença do wired headset entre os dispositivos elegíveis;
- MAIN atualmente selecionado;
- modo de áudio atual;
- capacidades advertised do fone.

Nenhum `setCommunicationDevice()` nesta fase.

Critério:
- no SM-X230, confirmar objetivamente se o fone cabeado é elegível como communication device.

## Fase 2 — baseline de MAIN

Abrir MAIN exatamente como no runtime atual:

- `USAGE_MEDIA`;
- `CONTENT_TYPE_MUSIC`;
- taxa do projeto;
- preferred device = MK-300;
- playback silencioso;
- provar `routedDevices == MK-300`.

Registrar clock, configuração, underruns e amplitude lógica do bus antes do CUE.

Critério:
- nenhum experimento prossegue sem MAIN comprovado.

## Fase 3 — Communication Split candidato A, sem mudar AudioManager mode

Criar CUE como segundo `AudioTrack` com:

- `USAGE_VOICE_COMMUNICATION`;
- inicialmente `CONTENT_TYPE_MUSIC` para preservar a intenção de conteúdo musical e evitar sinalizar speech desnecessariamente;
- PCM float estéreo;
- taxa preferencial compatível com o wired headset;
- `setCommunicationDevice(wired)`;
- sem alterar `AudioManager.mode`.

Não solicitar foco transitório ou ducking.

Após `play()`, provar simultaneamente:

- MAIN continua em MK-300;
- CUE está exclusivamente no wired headset;
- `getCommunicationDevice() == wired`.

Critério:
- se os dois streams não forem fisicamente distintos, candidato A falha.

## Fase 4 — candidato B com MODE_IN_COMMUNICATION, somente se necessário

Se A falhar por comunicação não assumir a rota, testar em probe isolado:

1. salvar `AudioManager.mode`;
2. definir `MODE_IN_COMMUNICATION`;
3. selecionar wired com `setCommunicationDevice`;
4. repetir prova física;
5. restaurar o modo anterior e limpar communication device em `finally`.

Este candidato tem risco maior porque o próprio Android documenta que o mode afeta routing, volume management e HAL state.

Critério:
- rejeitar se MAIN mudar de MK-300, se qualquer outra rota for afetada ou se restauração não for determinística.

## Fase 5 — gate de fidelidade

Separação física não é suficiente.

Para cada candidato que separar rotas, registrar no CUE:

- `AudioTrack.getSampleRate()`;
- `getChannelCount()`;
- `getAudioFormat()` / `getFormat()`;
- encoding;
- buffer;
- performance mode;
- timestamps;
- routed devices.

Teste físico mínimo:
- sinal L-only;
- sinal R-only;
- sinal estéreo;
- conteúdo musical de referência;
- ausência de mono involuntário;
- ausência de distorção/processamento evidente.

Teste físico robusto, quando houver loopback disponível:
- capturar a saída wired por uma entrada independente;
- comparar MEDIA baseline vs COMMUNICATION candidate;
- medir resposta espectral relativa, channel separation, nível, clipping e estabilidade.

Não declarar qualidade equivalente apenas porque o `AudioTrack` aceitou 48 kHz estéreo: o OEM pode aplicar processamento depois da interface do app.

Critério:
- qualquer degradação incompatível com guitarra de referência elimina esta estratégia.

## Fase 6 — política de foco sem ducking

O CUE não cria uma segunda política de áudio-foco que reduza MAIN.

Proibições:
- não usar `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`;
- não aplicar ramps automáticos de redução de MAIN;
- não mapear eventos de CUE para `setVolume` de MAIN;
- não usar volume de sistema para equilibrar buses.

O playback do GuitarLab mantém sua política global de foco. Se uma mudança de focus imposta por outro app/sistema exigir pause/stop por regra de plataforma, isso é tratado como evento externo, não como comportamento CUE.

Critério:
- iniciar/parar CUE não altera samples, ganho, meter ou track volume do MAIN.

## Fase 7 — backend runtime Communication Split

Somente após Fases 1–6 aprovadas:

- manter os buses atuais `mainMix` e `cueMix`;
- MAIN continua no `AudioTrack` MEDIA;
- CUE usa `AudioTrack` COMMUNICATION;
- reutilizar resampling adaptativo se as taxas físicas diferirem;
- normalizar clocks para a taxa do projeto;
- manter CUE non-blocking;
- manter drift guard e route listeners.

A estratégia deve ser expressa como capability própria, por exemplo:
`COMMUNICATION_SPLIT`.

## Fase 8 — lifecycle e restauração

Criar owner explícito da sessão de comunicação.

Na entrada:
- snapshot do communication device e mode;
- aplicar somente o mínimo necessário.

Na saída/cancel/error/topology change:
- `clearCommunicationDevice()`;
- restaurar mode se o GuitarLab o alterou;
- liberar CUE;
- revalidar MAIN.

Toda mutação global deve estar em `try/finally`/owner lifecycle e ser idempotente.

Critério:
- sair do Studio ou perder o fone não deixa o tablet preso em modo de comunicação.

## Fase 9 — semântica de volume e mixer

O mixer do GuitarLab continua sendo a autoridade de nível de conteúdo.

- track gain/pan/mute/solo permanecem iguais;
- routing MAIN/CUE/BOTH permanece igual;
- não adicionar duck automático;
- não mudar Master porque CUE iniciou;
- não mudar ganhos persistidos do projeto;
- `AudioTrack.setVolume` fica em unidade salvo mute/fail-safe técnico.

O Android pode aplicar volume groups diferentes a MEDIA e COMMUNICATION. Isso deve aparecer em diagnóstico, mas o GuitarLab não deve ajustar automaticamente esses volumes.

Critério:
- o mesmo projeto conserva exatamente seus gains antes/durante/depois de CUE.

## Fase 10 — diagnostics

Adicionar ao bundle:

- strategy = `COMMUNICATION_SPLIT`;
- current audio mode;
- mode modified by GuitarLab?;
- available communication devices;
- selected communication device;
- MAIN/CUE AudioAttributes;
- content types;
- requested/effective formats;
- routed physical keys;
- sample rates/channels/encoding;
- audio focus policy;
- `duckingRequested=false`;
- MAIN logical gain before/during CUE;
- lifecycle cleanup status;
- fidelity qualification status.

## Fase 11 — testes automatizados

JVM:
- candidate ordering;
- state machine;
- no-duck invariant;
- restoration policy;
- routing/fidelity result classification.

Android/API 36:
- communication discovery;
- unavailable wired route;
- lifecycle cleanup;
- config changes;
- app background/foreground where legal;
- route loss;
- mode restoration;
- mixer gains unchanged.

Adicionar guards de CI que impeçam introdução acidental de
`AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` no caminho CUE.

## Fase 12 — matriz física de homologação

No SM-X230:

A. MAIN MK-300 / CUE wired, candidato A;
B. MAIN MK-300 / CUE wired, candidato B somente se A falhar;
C. MAIN speaker / CUE wired;
D. opcionalmente MAIN MK-300 / CUE speaker para comparar com o comportamento observado em Android Auto/communication.

Para cada combinação registrar:
- rota;
- formato;
- qualidade;
- estabilidade;
- seek/loop;
- long run;
- disconnect/reconnect;
- recording backing;
- ausência de ducking;
- retorno ao estado normal após cleanup.

## Regra de promoção

`COMMUNICATION_SPLIT` só vira recurso suportado se, no hardware real:

1. MAIN e CUE permanecerem fisicamente distintos;
2. MAIN não sofrer ducking, pause ou ganho implícito;
3. CUE preservar qualidade aprovada para guitarra de referência;
4. mixer permanecer a única automação de ganho do GuitarLab;
5. lifecycle/restauração forem determinísticos;
6. CI e API36 estiverem verdes;
7. owner acceptance for registrada.

Caso a rota funcione mas a fidelidade não, o resultado é
`ROUTE_SUPPORTED_QUALITY_REJECTED`, não `SUPPORTED`.

## Relação com USB multicanal

Mesmo se Communication Split for aprovado, USB multicanal continua sendo o caminho profissional preferido quando disponível:

- um único dispositivo;
- um único clock;
- canais físicos explícitos;
- sem depender de políticas de comunicação do OEM.

Communication Split é uma capability oportunista para ampliar compatibilidade de hardware existente.
