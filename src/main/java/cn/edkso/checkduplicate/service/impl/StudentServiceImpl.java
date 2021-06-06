package cn.edkso.checkduplicate.service.impl;

import cn.edkso.checkduplicate.constant.ExceptionDefault;
import cn.edkso.checkduplicate.dao.StudentDao;
import cn.edkso.checkduplicate.entry.Clazz;
import cn.edkso.checkduplicate.entry.Student;
import cn.edkso.checkduplicate.exception.CDException;
import cn.edkso.checkduplicate.service.ClazzService;
import cn.edkso.checkduplicate.service.StudentService;
import cn.edkso.utils.MD5Utils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.util.List;

@Service
@Transactional
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentDao studentDao;

    @Autowired
    private ClazzService clazzService;

    @Override
    public Student login(String username, String password) {
        Student student = studentDao.findByUsernameAndPassword(username, MD5Utils.md5(password));
        if(student != null){
            return student;
        }
        return null;
    }

    @Override
    public Student register(String username, String password, String name) {
        Student student = new Student();
        student.setState(1);
        student.setName(name);
        student.setUsername(username);
        student.setPassword(MD5Utils.md5(password));
        student.setName(name);
        Student resStudent = studentDao.save(student);
        return resStudent;
    }

    @Override
    public List<Student> findAllByClazzId(Integer id) {
        return studentDao.findAllByClazzId(id);
    }

    @Override
    public Student update(Student oldStudent) {

        if(oldStudent.getClazzId() != null){
            Clazz clazz = clazzService.findById(oldStudent.getClazzId());
            oldStudent.setClazzName(clazz.getName());
        }

        return studentDao.save(oldStudent);
    }

    @Override
    public Page<Student> listByPage(Integer page, Integer limit, Student student) {
        Pageable pageable = PageRequest.of(page -1,limit);

        Specification specification = (root, cq, cb) -> {
            Predicate predicate = cb.conjunction();
            //增加筛选条件0(学生名称模糊匹配)
            if (StringUtils.isNotBlank(student.getName())){
                predicate.getExpressions().add(cb.like(root.get("name"), "%" + student.getName() + "%"));
            }
            //增加筛选条件1(学生班级Id精确匹配)
            if (student.getClazzId() != null){
                predicate.getExpressions().add(cb.equal(root.get("clazzId"), student.getClazzId()));
            }
            return predicate;
        };

        Page<Student> studentPage = studentDao.findAll(specification, pageable);
        return studentPage;
    }

    @Override
    public Student updateForManager(Student student) {
        Student oldStudentr = studentDao.findById(student.getId()).get();

        if (StringUtils.isNotBlank(student.getName())){
            oldStudentr.setName(student.getName());
        }
        if (StringUtils.isNotBlank(student.getPassword())){
            oldStudentr.setPassword(MD5Utils.md5(student.getPassword()));
        }
        if (student.getState() != null){
            oldStudentr.setState(student.getState());
        }
        if (student.getClazzId() != null){
            oldStudentr.setClazzId(student.getClazzId());
            Clazz clazz = clazzService.findById(student.getClazzId());
            oldStudentr.setClazzName(clazz.getName());
        }
        return studentDao.save(oldStudentr);
    }

    @Override
    public void delForManager(List<Student> studentList) {
        try {
            studentDao.deleteAll(studentList);
        } catch (Exception e){
            throw new CDException(ExceptionDefault.DEL_ERROR);
        }
    }


}
