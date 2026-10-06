# CUE USB multicanal — plano de implementação

Updated: 2026-10-06

## Objetivo

Implementar um backend de saída profissional e determinístico em que o GuitarLab use um único dispositivo USB multicanal e faça o roteamento internamente:

- MAIN -> canais USB 1/2;
- CUE -> canais USB 3/4;
- um único clock físico;
- um único dispositivo Android;
- sem depender de duas estratégias de media concorrentes ou de duas rotas físicas independentes controladas pela AudioPolicy do OEM.

Este plano complementa, não substitui, o modo CUE multi-device já existente. O modo multi-device continua capability-based e fail-closed; o novo modo USB multicanal será preferido quando houver capacidade comprovada.

## Invariantes

1. A taxa do projeto/timeline continua autoritativa.
2. Nenhum arquivo de projeto é regravado ou convertido apenas para satisfazer a saída física.
3. MAIN e CUE continuam buses independentes do mixer.
4. O CUE nunca pode vazar silenciosamente para MAIN.
5. Nenhum suporte é declarado apenas por capability advertised; é necessário preflight real.
6. Desconexão, mudança de topologia ou perda de capacidade invalida o perfil ativo.
7. O controle de nível continua pertencendo ao mixer do GuitarLab. Não deve existir ducking automático entre MAIN e CUE.
8. A implementação deve permanecer compatível com o fluxo atual de playback, seek, loop, recording backing, meters e fail-safe.

## Arquitetura alvo

```text
                         GuitarLab
                            |
                   mixer / timeline
                            |
                +-----------+-----------+
                |                       |
             MAIN bus                CUE bus
                |                       |
              L / R                   L / R
                |                       |
                +-----------+-----------+
                            |
                 interleaving 4 canais
                            |
                     AudioTrack USB
                            |
                      [1][2][3][4]
                       |  |  |  |
                       +--+  +--+
                       MAIN  CUE
```

## Estratégia 1 — modelo formal de capacidade e backend

Introduzir uma abstração explícita de estratégia de CUE, separando pelo menos:

- `MULTI_DEVICE`: dois dispositivos Android independentes, comportamento atual;
- `USB_MULTICHANNEL`: um único dispositivo USB com pelo menos quatro canais de playback;
- `UNAVAILABLE`: nenhuma rota comprovada.

O backend selecionado deve ser consequência de capability detection e homologação, nunca uma configuração presumida.

Entregáveis:
- tipo selado/enum de estratégia;
- estado de capability separado de preferência de usuário;
- integração com `CueRouteController`;
- migração sem quebrar preferências existentes.

Critério de aceite:
- o app consegue explicar por diagnóstico qual estratégia foi escolhida e por quê.

## Estratégia 2 — UsbMultichannelCapabilityProbe

Criar um probe dedicado para dispositivos USB de saída.

Coletar, quando disponíveis:
- `AudioDeviceInfo.getChannelCounts()`;
- `getChannelMasks()`;
- `getChannelIndexMasks()`;
- `getSampleRates()`;
- `getEncodings()`;
- `getAudioProfiles()`;
- em API compatível, `AudioManager.getSupportedMixerAttributes(device)`.

O probe deve distinguir:
- capability desconhecida;
- estéreo apenas;
- quatro ou mais canais anunciados;
- combinação de sample rate / encoding / channel mask potencialmente utilizável.

Critério de aceite:
- o diagnóstico mostra a capacidade USB bruta e a candidata normalizada sem inferir suporte que não tenha sido provado.

## Estratégia 3 — preflight físico real

Capability advertised é apenas entrada da negociação.

Para cada candidato plausível:
1. criar um `AudioTrack` silencioso com a configuração multicanal;
2. aplicar o USB como preferred device;
3. iniciar playback silencioso;
4. verificar `routedDevice(s)`;
5. verificar inicialização, writes, timestamps e underruns;
6. qualificar estabilidade por janela limitada;
7. encerrar e liberar tudo.

Só uma configuração que conclua esse preflight pode virar perfil suportado.

Critério de aceite:
- nenhum `USB_MULTICHANNEL` é persistido sem preflight real concluído.

## Estratégia 4 — UsbMultichannelOutputProfile

Criar um perfil comprovado contendo, no mínimo:

- identidade física/canônica do dispositivo;
- sample rate físico;
- encoding;
- channel count;
- channel mask / channel index mask;
- par MAIN;
- par CUE;
- mixer attributes efetivos;
- Android/API/device fingerprint relevante;
- evidência de preflight;
- versão do schema.

O mapeamento default poderá ser MAIN 1/2 e CUE 3/4, porém deve ser configurável/homologável porque interfaces podem expor ordens diferentes.

Critério de aceite:
- o perfil pode ser serializado para diagnóstico e invalidado de forma determinística.

## Estratégia 5 — renderer multicanal de sink único

Generalizar o renderer para continuar produzindo dois buses estéreo independentes:

- `mainMix[L,R]`;
- `cueMix[L,R]`.

No backend USB multicanal, intercalar em um único frame físico:

```text
frame N:
ch1 = MAIN-L
ch2 = MAIN-R
ch3 = CUE-L
ch4 = CUE-R
```

Deve existir uma implementação pura/testável do interleaver, separada de Android APIs.

Critério de aceite:
- testes unitários provam ordem de canal e ausência de cross-talk lógico.

## Estratégia 6 — negociação de taxa física

Preservar a taxa do projeto.

Se o projeto estiver em 44.1 kHz e o USB multicanal comprovado operar em 48 kHz:
- resample MAIN e CUE na borda física;
- intercalar somente depois da adaptação;
- manter timeline, clips, recording e edição na taxa original.

Reutilizar/adaptar a infraestrutura de resampling criada no rc30, evitando duplicação de lógica.

Critério de aceite:
- duração temporal e posição do playhead permanecem na base do projeto, sem drift acumulativo.

## Estratégia 7 — matriz de roteamento interna

`TrackOutputRoute` continua sendo a autoridade semântica:

- MAIN -> somente bus MAIN;
- CUE -> somente bus CUE;
- BOTH -> ambos.

O backend apenas decide como os buses chegam ao hardware.

Critério de aceite:
- trocar entre `MULTI_DEVICE` e `USB_MULTICHANNEL` não muda a semântica de routing das pistas.

## Estratégia 8 — nível CUE independente

Separar claramente:
- ganho de pista;
- Master/Main gain;
- Cue master gain.

Não criar ducking automático entre MAIN e CUE.

O usuário deve controlar os níveis pelo mixer do GuitarLab. A ativação do CUE não pode reduzir MAIN nem elevar/reduzir CUE de forma implícita.

Critério de aceite:
- ativar/desativar CUE não muda ganho lógico de MAIN;
- ambos os níveis podem ser ajustados manualmente e permanecem previsvisíveis.

## Estratégia 9 — assistente de homologação de canais

O Android pode informar quatro canais sem dizer de forma confiável qual conector físico corresponde a 1/2 ou 3/4.

Adicionar um wizard de homologação:
1. tocar sinal de teste somente em 1/2;
2. usuário confirma destino físico;
3. tocar sinal somente em 3/4;
4. usuário confirma destino físico;
5. persistir o mapeamento homologado por identidade de interface.

O wizard deve usar sinal seguro, limitado, claramente identificável e nunca áudio de projeto.

Critério de aceite:
- o usuário consegue provar MAIN e CUE em conectores físicos diferentes antes de usar o perfil em sessão real.

## Estratégia 10 — persistência e invalidação do perfil

Persistir apenas dados estáveis:
- identidade canônica;
- configuração comprovada;
- mapeamento confirmado pelo usuário.

Invalidar/revalidar quando:
- dispositivo é removido;
- identidade física muda;
- capabilities mudam;
- Android/build relevante muda;
- mixer attributes deixam de existir;
- track falha ao abrir;
- runtime route diverge.

Cache nunca substitui a revalidação curta antes do playback musical.

Critério de aceite:
- perfil stale nunca gera falso `SUPPORTED`.

## Estratégia 11 — diagnóstico e observabilidade

Expandir `audio-route.json` com uma seção USB multicanal contendo:

- device identity sanitizada;
- advertised channel counts;
- channel masks;
- channel index masks;
- sample rates;
- encodings;
- audio profiles;
- supported mixer attributes;
- candidatos tentados;
- configuração escolhida;
- channel mapping;
- resultado do preflight;
- runtime track configuration;
- routed device;
- underruns;
- motivo de fallback/failure.

Exemplo esperado:

```text
strategy=USB_MULTICHANNEL
device=USB Audio Interface
advertisedChannelCounts=[2,4]
negotiatedSampleRateHz=48000
negotiatedChannelCount=4
mainChannels=[1,2]
cueChannels=[3,4]
preflight=SUPPORTED
mapping=USER_CONFIRMED
```

Critério de aceite:
- um bundle de diagnóstico deve ser suficiente para explicar por que uma interface foi aceita ou rejeitada.

## Estratégia 12 — testes, homologação e promoção

### JVM/unit
Cobrir:
- normalização de capabilities;
- ordenação de candidatos;
- channel-mask/index-mask selection;
- interleaver;
- routing matrix;
- resampling;
- seek/reset;
- profile invalidation;
- fallback;
- zero cross-talk lógico.

### Android/API 36
Cobrir:
- discovery sem USB;
- USB estéreo apenas;
- dispositivo mock/fake capability;
- lifecycle;
- disconnect/reconnect;
- Settings/diagnostics.

### Hardware
Para cada interface candidata:
1. provar 4+ playback channels;
2. homologar 1/2 e 3/4;
3. tocar sinais diferentes simultaneamente;
4. validar estabilidade;
5. validar seek/loop;
6. validar gravação com backing;
7. observar underruns;
8. confirmar que não há ducking nem alteração implícita de volume;
9. exportar bundle de diagnóstico.

### Critério de promoção
O recurso só pode ser anunciado como suportado para uma topologia quando:
- CI verde;
- preflight multicanal verde;
- channel mapping confirmado;
- hardware real reproduziu MAIN e CUE simultaneamente;
- nenhum vazamento/cross-talk;
- nenhum ducking automático;
- owner acceptance registrada.

## Sequência recomendada de execução

1. tipos de estratégia/capability;
2. capability scanner;
3. normalização/testes de capabilities;
4. preflight multicanal;
5. perfil comprovado;
6. interleaver + resampling;
7. backend runtime;
8. Cue master gain;
9. wizard de channel mapping;
10. diagnóstico;
11. instrumentação;
12. homologação física.

## Não objetivos

- não forçar uma interface estéreo a se comportar como 4 canais;
- não assumir que headphone físico implica canais USB independentes;
- não usar dois dispositivos USB como requisito do modo profissional;
- não promover suporte com base apenas em datasheet;
- não substituir a arquitetura de projeto/timeline por uma taxa física;
- não aceitar fallback que faça CUE aparecer em MAIN.

## Relação com o modo multi-device atual

O rc30 continua relevante. O fluxo futuro deve preferir:

```text
CUE solicitado
  |
  +-- USB multicanal comprovado? -> USB_MULTICHANNEL
  |
  +-- dois devices independentes comprovados? -> MULTI_DEVICE
  |
  +-- nenhum -> UNAVAILABLE
```

Outras estratégias experimentais podem ser investigadas separadamente, mas não alteram os critérios de promoção deste plano.
