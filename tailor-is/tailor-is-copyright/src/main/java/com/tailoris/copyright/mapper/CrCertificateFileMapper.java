package com.tailoris.copyright.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.copyright.entity.CrCertificateFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CrCertificateFileMapper extends BaseMapper<CrCertificateFile> {

    @Select("SELECT * FROM cr_certificate_file WHERE cert_no = #{certNo} AND deleted = 0")
    CrCertificateFile selectByCertNo(@Param("certNo") String certNo);

    @Select("SELECT * FROM cr_certificate_file WHERE record_id = #{recordId} AND deleted = 0 LIMIT 1")
    CrCertificateFile selectByRecordId(@Param("recordId") Long recordId);

    @Update("UPDATE cr_certificate_file SET download_count = download_count + 1 WHERE id = #{id}")
    int incrDownloadCount(@Param("id") Long id);
}
