package com.jan1ooo.cbf.campeonatobrasileiro.service;

import com.jan1ooo.cbf.campeonatobrasileiro.DTO.ClassificacaoDTO;
import com.jan1ooo.cbf.campeonatobrasileiro.DTO.ClassificacaoTimeDTO;
import com.jan1ooo.cbf.campeonatobrasileiro.DTO.JogoDTO;
import com.jan1ooo.cbf.campeonatobrasileiro.DTO.TimeDTO;
import com.jan1ooo.cbf.campeonatobrasileiro.DTO.mapper.JogoMapper;
import com.jan1ooo.cbf.campeonatobrasileiro.DTO.mapper.TimeMapper;
import com.jan1ooo.cbf.campeonatobrasileiro.entity.Jogo;
import com.jan1ooo.cbf.campeonatobrasileiro.repository.JogoRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class JogoService {

    @Autowired
    private final JogoRepository jogoRepository;

    @Autowired
    JogoMapper jogoMapper;

    @Autowired
    TimeService timeService;

    @Autowired
    TimeMapper timeMapper;

    public JogoService(JogoRepository jogoRepository) {
        this.jogoRepository = jogoRepository;
    }

    public List<JogoDTO> obterJogos() {
        return jogoRepository.findAll()
                .stream()
                .map(jogoMapper::toDTO)
                .collect(Collectors.toList());
    }

    public JogoDTO create(@Valid @NotNull JogoDTO jogoDTO) {
        return jogoMapper.toDTO(jogoRepository.save(jogoMapper.toEntity(jogoDTO)));
    }

    public void gerarJogos(LocalDateTime primeiraRodada) {
        jogoRepository.deleteAll();

        final List<TimeDTO> times = timeService.findAll();
        List<TimeDTO> all1 = new ArrayList<>();
        List<TimeDTO> all2 = new ArrayList<>();
        all1.addAll(times);//.subList(0, times.size()/2));
        all2.addAll(times);//.subList(all1.size(), times.size()));

        List<Jogo> jogos = new ArrayList<>();

        int t = times.size();
        int m = times.size() / 2;
        LocalDateTime dataJogo = primeiraRodada;
        Integer rodada = 0;
        for (int i = 0; i < t - 1; i++) {
            rodada += 1 + i;
            for (int j = 0; j < m; j++) {
                //Teste para ajustar o mando de campo
                TimeDTO time1;
                TimeDTO time2;
                if (j % 2 == 1 || i % 2 == 1 && j == 0) {
                    time1 = times.get(t - j - 1);
                    time2 = times.get(j);
                } else {
                    time1 = times.get(j);
                    time2 = times.get(t - j - 1);
                }
                if (time1 == null) {
                    System.out.println("Time  1 null");
                }
                jogos.add(jogoMapper.toEntity(gerarJogo(dataJogo, rodada, time1, time2)));
                dataJogo = dataJogo.plusDays(7);
            }
            //Gira os times no sentido horário, mantendo o primeiro no lugar
            times.add(1, times.remove(times.size() - 1));
        }

        jogos.forEach(jogo -> System.out.println(jogo));

        jogoRepository.saveAll(jogos);

        List<Jogo> jogos2 = new ArrayList<>();

        Integer finalRodada = rodada;
        jogos.forEach(jogo -> {
            TimeDTO timeCasa = timeMapper.toDTO(jogo.getTimeFora());
            TimeDTO timeFora = timeMapper.toDTO(jogo.getTimeCasa());
            jogos2.add(jogoMapper.toEntity(gerarJogo(jogo.getData().plusDays(7L * jogos.size()), finalRodada + 1, timeCasa, timeFora)));
        });
        jogoRepository.saveAll(jogos2);
    }

    private JogoDTO gerarJogo(LocalDateTime dataJogo, Integer rodada, TimeDTO timeCasa, TimeDTO timeFora) {
        Jogo jogo = new Jogo();
        jogo.setTimeCasa(timeMapper.toEntity(timeCasa));
        jogo.setTimeFora(timeMapper.toEntity(timeFora));
        jogo.setRodada(rodada);
        jogo.setData(dataJogo);
        jogo.setGolsTimeCasa(0);
        jogo.setGolsTimeFora(0);
        jogo.setPublicoPagante(0);
        jogo.setEncerrado(false);

        return jogoMapper.toDTO(jogo);
    }

    public JogoDTO obterJogo(Long id) {
        return jogoMapper.toDTO(jogoRepository.findById(id).get());
    }

    public void finalizarJogo(Long id, JogoDTO jogoDTO) throws Exception {
        Jogo jogo = jogoRepository.findById(id).orElseThrow(() -> new Exception("Jogo não encontrado"));
        if (jogo.getEncerrado()) {
            throw new Exception("Jogo já foi encerrado");
        }
        jogo.setGolsTimeCasa(jogoDTO.getGolsTimeCasa());
        jogo.setGolsTimeFora(jogoDTO.getGolsTimeFora());
        jogo.setEncerrado(true);
        jogo.setPublicoPagante(jogoDTO.getPublicoPagante());
        jogo.setRodada(jogoDTO.getRodada());
        jogoRepository.save(jogo);
    }

    public void deleteAll() {
        jogoRepository.deleteAll();
    }

    public ClassificacaoDTO obterClassificacao() {
        ClassificacaoDTO classificacaoDTO = new ClassificacaoDTO();
        List<TimeDTO> times = timeService.findAll();

        times.forEach(time -> {
            List<Jogo> jogoTimeCasa = jogoRepository.findByTimeCasaAndEncerrado(timeMapper.toEntity(time), true);
            List<Jogo> jogoTimeFora = jogoRepository.findByTimeForaAndEncerrado(timeMapper.toEntity(time), true);
            AtomicReference<Integer> vitorias = new AtomicReference<>(0);
            AtomicReference<Integer> empates = new AtomicReference<>(0);
            AtomicReference<Integer> derrotas = new AtomicReference<>(0);
            AtomicReference<Integer> golsMarcados = new AtomicReference<>(0);
            AtomicReference<Integer> golsSofridos = new AtomicReference<>(0);
            jogoTimeCasa.forEach(jogo -> {
                if (jogo.getGolsTimeCasa() > jogo.getGolsTimeFora()) {
                    vitorias.getAndSet(vitorias.get() + 1);
                } else if (jogo.getGolsTimeCasa() < jogo.getGolsTimeFora()) {
                    derrotas.getAndSet(derrotas.get() + 1);
                } else {
                    empates.getAndSet(empates.get() + 1);
                }
                golsMarcados.set(golsMarcados.get() + jogo.getGolsTimeCasa());
                golsSofridos.set(golsSofridos.get() + jogo.getGolsTimeFora());
            });
            jogoTimeFora.forEach(jogo -> {
                if (jogo.getGolsTimeFora() > jogo.getGolsTimeCasa()) {
                    vitorias.getAndSet(vitorias.get() + 1);
                } else if (jogo.getGolsTimeFora() < jogo.getGolsTimeCasa()) {
                    derrotas.getAndSet(derrotas.get() + 1);
                } else {
                    empates.getAndSet(empates.get() + 1);
                }
                golsMarcados.set(golsMarcados.get() + jogo.getGolsTimeFora());
                golsSofridos.set(golsSofridos.get() + jogo.getGolsTimeCasa());
            });
            ClassificacaoTimeDTO classificacaoTimeDTO = new ClassificacaoTimeDTO();
            classificacaoTimeDTO.setId_time(time.id_time());
            classificacaoTimeDTO.setTime(time.nome());
            classificacaoTimeDTO.setPontos((vitorias.get() * 3) + empates.get());
            classificacaoTimeDTO.setDerrotas(derrotas.get());
            classificacaoTimeDTO.setEmpates(empates.get());
            classificacaoTimeDTO.setVitorias(vitorias.get());
            classificacaoTimeDTO.setGolsMarcados(golsMarcados.get());
            classificacaoTimeDTO.setGolsSofridos(golsSofridos.get());
            classificacaoTimeDTO.setJogos(derrotas.get() + empates.get() + vitorias.get());
            classificacaoDTO.getTimes().add(classificacaoTimeDTO);
        });

        Collections.sort(classificacaoDTO.getTimes(), Collections.reverseOrder());
        int posicao = 0;
        for (ClassificacaoTimeDTO time : classificacaoDTO.getTimes()) {
            time.setPosicao(posicao++);
        }
//        classificacaoDTO.getTimes().sort();

        return classificacaoDTO;
    }
}
