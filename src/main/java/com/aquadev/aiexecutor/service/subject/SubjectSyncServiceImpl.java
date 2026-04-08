package com.aquadev.aiexecutor.service.subject;

import com.aquadev.aiexecutor.model.Subject;
import com.aquadev.aiexecutor.model.Teacher;
import com.aquadev.aiexecutor.repository.SubjectRepository;
import com.aquadev.aiexecutor.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SubjectSyncServiceImpl implements SubjectSyncService {

    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;

    @Override
    @Transactional
    public void sync(Long apiSubjectId, String name, String teacherFio) {
        Teacher teacher = null;
        if (StringUtils.hasText(teacherFio)) {
            teacher = teacherRepository.findByFio(teacherFio)
                    .orElseGet(() -> {
                        Teacher t = new Teacher();
                        t.setFio(teacherFio);
                        return teacherRepository.save(t);
                    });
        }

        Subject subject = subjectRepository.findByApiSubjectId(apiSubjectId)
                .orElseGet(() -> {
                    Subject s = new Subject();
                    s.setApiSubjectId(apiSubjectId);
                    return s;
                });
        subject.setName(name);
        subject.setTeacher(teacher);
        subjectRepository.save(subject);
    }
}
