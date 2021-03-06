package cn.dreampie.common.web.controller;

import cn.dreampie.common.config.ReTurnType;
import cn.dreampie.common.plugin.patchca.PatchcaRender;
import cn.dreampie.common.plugin.shiro.hasher.Hasher;
import cn.dreampie.common.plugin.shiro.hasher.HasherInfo;
import cn.dreampie.common.plugin.shiro.hasher.HasherUtils;
import cn.dreampie.common.web.thread.ThreadLocalUtil;
import cn.dreampie.common.utils.SubjectUtils;
import cn.dreampie.common.utils.ValidateUtils;
import cn.dreampie.function.user.User;
import com.jfinal.aop.Before;
import com.jfinal.plugin.activerecord.tx.Tx;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import java.util.Date;

/**
 * Controller
 */
public class Controller extends com.jfinal.core.Controller {

    public void dynaRender(String view) {
        if (ThreadLocalUtil.returnType() == ReTurnType.JSON)
            super.renderJson();
        else
            super.render(view);
    }


    /**
     * 根目录
     */
//  @Before(EvictInterceptor.class)
//  @CacheName("index")
    public void index() {
        dynaRender("/page/index.ftl");
    }


    /**
     * 登录页
     */
    public void tologin() {
        Subject subject = SecurityUtils.getSubject();
        if (subject != null && subject.getPrincipal() != null) {
            subject.logout();
        }
        dynaRender("/page/login.ftl");
    }

    public void toregister() {
        dynaRender("/page/register.ftl");
    }

    /**
     * 验证码
     */
    public void patchca() {
        int width = 0, height = 0, minnum = 0, maxnum = 0;
        if (isParaExists("width")) {
            width = getParaToInt("width");
        }
        if (isParaExists("height")) {
            height = getParaToInt("height");
        }
        if (isParaExists("minnum")) {
            minnum = getParaToInt("minnum");
        }
        if (isParaExists("maxnum")) {
            maxnum = getParaToInt("maxnum");
        }
        render(new PatchcaRender(minnum, maxnum, width, height));
    }

    @Before({RootValidator.RegisterValidator.class, Tx.class})
    public void register() {
        User regUser = getModel(User.class);
        regUser.set("created_at", new Date());
        regUser.set("providername", "dreampie");

        boolean autoLogin = getParaToBoolean("autoLogin");

        if (!ValidateUtils.me().isNullOrEmpty(regUser.get("first_name")) || !ValidateUtils.me().isNullOrEmpty(regUser.get("last_name"))) {
            regUser.set("full_name", regUser.get("first_name") + "·" + regUser.get("last_name"));
        }

        HasherInfo passwordInfo = HasherUtils.me().hash(regUser.getStr("password"), Hasher.DEFAULT);
        regUser.set("password", passwordInfo.getHashResult());
        regUser.set("hasher", passwordInfo.getHasher().value());
        regUser.set("salt", passwordInfo.getSalt());

        if (regUser.save()) {
            regUser.addUserInfo(null).addRole(null);
            setAttr("state", "success");
            if (autoLogin) {
                if (SubjectUtils.me().login(regUser.getStr("username"), passwordInfo.getHashText())) {
                    dynaRender("/page/index.ftl");
                } else
                    dynaRender("/page/login.ftl");
            }
        } else {
            setAttr("state", "failure");
            dynaRender("/page/register.ftl");
        }
    }

}
