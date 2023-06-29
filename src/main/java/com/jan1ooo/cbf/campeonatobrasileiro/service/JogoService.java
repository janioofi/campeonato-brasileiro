package com.jan1ooo.cbf.campeonatobrasileiro.service;

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
        final List<TimeDTO> times = timeService.findAll();
        List<TimeDTO> all1 = new ArrayList<>();
        List<TimeDTO> all2 = new ArrayList<>();
        all1.addAll(times);//.subList(0, times.size()/2));
        all2.addAll(times);//.subList(all1.size(), times.size()));

        jogoRepository.deleteAll();

        List<Jogo> jogos = new ArrayList<>();

        int t = times.size();
        int m = times.size() / 2;
        LocalDateTime dataJogo = primeiraRodada;
        AtomicReference<Integer> rodada = new AtomicReference<>(0);
        for (int i = 0; i < t - 1; i++) {
            rodada.set(i + 1);
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
                jogos.add(jogoMapper.toEntity(gerarJogo(dataJogo, rodada.get(), time1, time2)));
                dataJogo = dataJogo.plusDays(7);
            }
            //Gira os times no sentido horário, mantendo o primeiro no lugar
            times.add(1, times.remove(times.size() - 1));
        }

        jogos.forEach(jogo -> System.out.println(jogo));

        jogoRepository.saveAll(jogos);

        List<Jogo> jogos2 = new ArrayList<>();

        jogos.forEach(jogo -> {
            Integer rod = jogos.size();
            TimeDTO timeCasa = timeMapper.toDTO(jogo.getTimeFora());
            TimeDTO timeFora = timeMapper.toDTO(jogo.getTimeCasa());
            jogos2.add(jogoMapper.toEntity(gerarJogo(jogo.getData().plusDays(7L * jogos.size()), rod += 1, timeCasa, timeFora)));
        });
        jogoRepository.saveAll(jogos2);
    }

    private JogoDTO gerarJogo(LocalDateTime dataJogo, Integer rodada, TimeDTO timeCasa, TimeDTO timeFora) {
        Jogo jogo = new Jogo();
        jogo.setTimeCasa(timeMapper.toEntity(timeCasa));
        jogo.setTimeFora(timeMapper.toEntity(timeFora));
        jogo.setRodada(1);
        jogo.setData(dataJogo);
        jogo.setGolsTimeCasa(3);
        jogo.setGolsTimeFora(0);
        jogo.setPublicoPagante(40000);
        jogo.setEncerrado(false);

        return jogoMapper.toDTO(jogo);
    }
}