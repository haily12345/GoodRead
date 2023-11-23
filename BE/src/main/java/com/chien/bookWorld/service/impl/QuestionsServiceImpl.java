package com.chien.bookWorld.service.impl;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.chien.bookWorld.dto.OptionDto;
import com.chien.bookWorld.dto.QuestionsDto;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.chien.bookWorld.dto.QuestionsCreationDto;
import com.chien.bookWorld.entity.Options;
import com.chien.bookWorld.entity.Questions;
import com.chien.bookWorld.entity.Scoring;
import com.chien.bookWorld.entity.UserDetailsImpl;
import com.chien.bookWorld.exception.AppException;
import com.chien.bookWorld.payload.response.SuccessResponse;
import com.chien.bookWorld.repository.BookRepository;
import com.chien.bookWorld.repository.OptionsRepository;
import com.chien.bookWorld.repository.QuestionsRepository;
import com.chien.bookWorld.repository.ScoringRepository;
import com.chien.bookWorld.repository.UserRepository;
import com.chien.bookWorld.service.QuestionsService;

@Service
public class QuestionsServiceImpl implements QuestionsService {

    @Autowired
    private QuestionsRepository questionsRepository;

    @Autowired
    private OptionsRepository optionsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ScoringRepository scoringRepository;

    @Autowired
    private ModelMapper mapper;

    private static final Logger logger = Logger.getLogger(QuestionsServiceImpl.class.getName());

    @Override
    public Map<String, Object> create(QuestionsCreationDto c) {
        // TODO Auto-generated method stub
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList();

        boolean isAuthor = roles.contains("ROLE_AUTHOR");

        if (!isAuthor) {
            throw new AppException(403, 43, "Cần quyền là tác giả.");
        }
        Questions questions = new Questions();
        UUID idQuestion = UUID.randomUUID();
        questions.setId(idQuestion);
        questions.setBook(bookRepository.findById(c.getIdBook())
                .orElseThrow(() -> new AppException(404, 44, "Không có cuốn sách phù hợp với id book")));
        questions.setQuestionText(c.getQuestionsText());
        questions.setScoring(c.getScoring());
        questionsRepository.save(questions);
        Set set = c.getOptionText().keySet();
        for (Object key : set) {
            logger.info("tesstt" + key.toString());
            Integer value = c.getOptionText().get(key);
            Options option = new Options();
            option.setIs_correct(value);
            option.setOptions_text(key.toString());
            option.setQuestions(questions);
            optionsRepository.save(option);
        }

        final Map<String, Object> body = new HashMap<>();
        body.put("code", 0);
        body.put("message", "Tạo câu hỏi thành công!");
        return body;
    }

    @Override
    public Map<String, Object> delete(Long id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SuccessResponse findAll() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SuccessResponse findById(Long id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object update(Questions u) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SuccessResponse getQuestionsByBook(Long idBook) {
        // TODO Auto-generated method stub
        List<Questions> questions = questionsRepository.getQuestionsByBookId(idBook);
        return new SuccessResponse(questions.stream().map(question -> {
            QuestionsDto questionsDto = mapper.map(question, QuestionsDto.class);
            questionsDto.setQuestionsText(question.getQuestionText());
            questionsDto.setIdBook(question.getBook().getId());

            List<Options> options = optionsRepository.findByQuestionId(question.getId());
            questionsDto.setOptionDtos(
                    options.stream().map(a -> mapper.map(a, OptionDto.class)).collect(
                            Collectors.toList())
            );
            return questionsDto;
        }).collect(
                Collectors.toList()));
    }

    public void updateScoring(Long idBook, Integer scoring) {
        // TODO Auto-generated method stub
        Scoring score = new Scoring();
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList();
        boolean isUser = roles.contains("ROLE_USER");
        if (!isUser) {
            throw new AppException(403, 43, "Tác giả và Admin không thể ghi điểm");
        }
        score.setBook(bookRepository.findById(idBook)
                .orElseThrow(() -> new AppException(404, 44, "Không có cuốn sách phù hợp với id book")));
        score.setUser(userRepository.findById(userDetails.getId()).orElseThrow(
                () -> new AppException(404, 44, "Không tìm thấy người dùng")));
        score.setScore(scoring);
        score.setTimestamp(new Timestamp(System.currentTimeMillis()));
        scoringRepository.save(score);

    }

    @Override
    public Map<String, Object> checkQuestion(Long idBook, Integer scoring, Long idAnswer, UUID idQuestion) {
        Options option = optionsRepository.findById(idAnswer).orElseThrow(() -> new AppException(404, 44,
                "Không tìm thấy câu trả lời ứng với id"));
        Integer isCorrect = option.getIs_correct();
        if (isCorrect == 0) {
            updateScoring(idBook, scoring);
            final Map<String, Object> body = new HashMap<>();
            body.put("code", 0);
            body.put("message", "Câu trả lời đúng!");
            return body;
        } else {
            final Map<String, Object> body = new HashMap<>();
            body.put("code", 0);
            body.put("message", "Câu trả lời sai!");
            return body;
        }
    }

}