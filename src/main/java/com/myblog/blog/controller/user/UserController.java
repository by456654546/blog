package com.myblog.blog.controller.user;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.myblog.blog.Service.BlogService;
import com.myblog.blog.Service.TypeService;
import com.myblog.blog.Service.UserService;
import com.myblog.blog.entity.Type;
import com.myblog.blog.entity.User;
import com.myblog.blog.quaryentity.*;
import com.myblog.blog.quaryentity.BlogQuery;
import com.myblog.blog.quaryentity.FirstPageBlog;
import com.myblog.blog.quaryentity.FollowEntity;
import com.myblog.blog.util.JsonResult;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.myblog.blog.Service.ex.*;

@Controller
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private BlogService blogService;
    @Autowired
    private TypeService typeService;
//注册
    @Value("${user.avatar}")
    String avatar;
    @Value("${user.introduction}")
    String introduction;
    @GetMapping("/register")
    public String register()
    {
        return "register";
    }
     @PostMapping("/register")
     public String reg(User user,Model model)
     {
         User user1 = userService.judgeUser(user.getUsername());
         if(user1!=null)
         {
             model.addAttribute("message","用户名已存在");
             return "register";
         }
         else{
             user.setIntroduction(introduction);
             user.setAvatar(avatar);
             userService.saveUser(user);
         }
        //注册成功进入登陆页面
        return "userLogin";
     }
     @GetMapping("/login")
     public  String login()
     {
         return "userLogin";
     }

@PostMapping("/login")
public  String dd(String username, String password, HttpSession session,
                  RedirectAttributes redirectAttributes)
{
    User user= userService.checkUser(username,password);
    System.out.println(username+password);
    if((user == null||username.equals("admin")))
    {
        redirectAttributes.addFlashAttribute("message","用户名或密码错误");
        return "redirect:/user/login";
        //失败重定向至原来页面
    }
    else {
        session.setAttribute("user",user);
        session.setMaxInactiveInterval(-1);    // 设置session永不过期
        return "redirect:/";
        //登陆成功进入下一页面 userIndex
    }
}

    /**
     * 关注
     * @param session
     * @param followId
     * @param model
     * @return
     */
    @GetMapping("/follow/{followId}")
    public String follow(HttpSession session,@PathVariable  long followId,Model model)
    {
        User user = (User)session.getAttribute("user");
        long userId=user.getId();
        int hasFollowed;
        if(user!=null)
        {
            userService.saveFollower(userId,followId);
            userService.updateFollow(userId);
            userService.updateFans(followId);
        }
        FollowEntity followed = userService.isFollowed(userId, followId);
        hasFollowed=followed==null?0:1;
        model.addAttribute("hasFollowed",hasFollowed);
        return "user :: message";
    }
    /**
     * 取消关注
     * @param session
     * @param followId
     * @param model
     * @return
     */
    @GetMapping("/unfollow/{followId}")
    public String unfollow(HttpSession session,@PathVariable  long followId,Model model)
    {
        User user = (User)session.getAttribute("user");
        long userId=user.getId();
        System.out.println("进去");
        int hasFollowed;
        if(user!=null)
        {
            userService.cancelFollow(userId,followId);
            userService.deleteFollow(userId);
            userService.deleteFans(followId);
        }
        FollowEntity followed = userService.isFollowed(userId, followId);
        hasFollowed=followed==null?0:1;
        model.addAttribute("hasFollowed",hasFollowed);
        System.out.println("出去");
        return "user :: message";
    }

    /**
     * 根据id得到用户的信息
     * @param userId
     * @param model
     * @return
     */
    @GetMapping("/{userId}")
    public String getDetailUser(@PathVariable Integer userId,@RequestParam(defaultValue = "1",value = "pageNum") Integer pageNum,Model model,HttpSession session)
    {
        User detailUser = userService.getDetailUser(userId);
        model.addAttribute("user",detailUser);
        User user = (User)session.getAttribute("user");
        Integer usrId=null;
        if(user!=null)
        {
            usrId=user.getId();
        }
        else {
            usrId=0;
        }
        int hasFollowed;
        FollowEntity followed = userService.isFollowed(usrId, userId);
        hasFollowed=followed==null?0:1;
        model.addAttribute("hasFollowed",hasFollowed);
        PageHelper.startPage(pageNum,4);
        List<BlogQuery> blogByUser = blogService.getBlogByUser(userId);
        PageInfo<BlogQuery> pageInfo = new PageInfo<>(blogByUser);
        model.addAttribute("pageInfo",pageInfo);
        List<Type> types = typeService.getAllTypesAndBlog();
        model.addAttribute("types",types);
        return "user";
    }
    /**
     * 搜索用户
     */
     @PostMapping("/UserSearch")
             public String getUser(@RequestParam String queryUser,
                     Model model, @RequestParam(defaultValue = "1",value = "pagenum") Integer pageNum)
     {
        List<UserQuery> userBySearch =userService.getUserBySearch(queryUser);
         System.out.println(userBySearch);
        PageHelper.startPage(pageNum,10);
        PageInfo<UserQuery> pageInfo =new PageInfo<>(userBySearch);
        model.addAttribute("pageInfo",pageInfo);
        model.addAttribute("queryUser",queryUser);
        return "searchuser";
     }
    /**
     * 进入自己的页面
     * @return
     */
    @GetMapping("/me")
    public String getMeMessage(HttpSession session,Model model,@RequestParam(defaultValue = "1",value = "pageNum") Integer pageNum)
    {
        User user = (User)session.getAttribute("user");
        User detailUser = userService.getDetailUser(user.getId());
        model.addAttribute("user",detailUser);
        String orderBy = "id desc";
        PageHelper.startPage(pageNum,5,orderBy);
        List<BlogQuery> list = blogService.getBlogByUser(user.getId());
        PageInfo<BlogQuery> pageInfo=new PageInfo<>(list);
        model.addAttribute("pageInfo",pageInfo);
        model.addAttribute("types",typeService.getTypes());
        return "userDetail";
    }
    @GetMapping("/followList")
    public String getFollow(HttpSession session,Model model)
    {
        User user =(User)session.getAttribute("user");
        List<User> users = userService.selectAllFollowers(user.getId());
        model.addAttribute("user",users);
        return "followList";
    }
    @PostMapping("/search")
    public String search(SearchUserBlog searchBlog, Model model,
                         @RequestParam(defaultValue = "1",value = "pageNum") Integer pageNum) {
        List<BlogQuery> blogBySearch = blogService.getUserSearchBlog(searchBlog);
        System.out.println(searchBlog);
        PageHelper.startPage(pageNum, 10);
        PageInfo<BlogQuery> pageInfo = new PageInfo<>(blogBySearch);
        model.addAttribute("pageInfo", pageInfo);
        return "user :: blogList";
    }
    /*
    * Bug
    * */
    @GetMapping("/unfollowList/{followId}")
    public String unfollowList(HttpSession session,@PathVariable  long followId,Model model)
    {
        User user = (User)session.getAttribute("user");
        long userId=user.getId();
        System.out.println(followId);
        if(user!=null)
        {
            userService.cancelFollow(userId,followId);
            userService.deleteFollow(userId);
            userService.deleteFans(followId);
        }
        List<User> users = userService.selectAllFollowers(user.getId());
        model.addAttribute("user",users);
        return "followList::message";
    }
    @GetMapping("/logout")
    public String logout(HttpSession session)
    {
        session.removeAttribute("user");
        return "redirect:/";
    }
    @PostMapping("/update")

    public String updateUser(User user)
    {
        System.out.println(user);
        userService.updateUser(user);
        return "redirect:/user/me";
    }
    @RequestMapping("/change_avatar")
    public JsonResult<String> changeAvatar(HttpSession session,
                                           @RequestParam("file") MultipartFile file) {

        if(file.isEmpty()) {
            try {
                throw new FileUploadException("文件为空");
            } catch (FileUploadException e) {
                e.printStackTrace();
            }
        }
        //
        String realPath = session.getServletContext().getRealPath("/upload");
        File dir = new File(realPath);
        if(!dir.exists()) {
            dir.mkdirs();//创建当前目录
        }
//        获取文件名称
        String originalFilename = file.getOriginalFilename();
        //获取文件名后缀
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
//        使用UUID随机生成一个新的文件名称前缀，拼接文件名称
        String filename = UUID.randomUUID().toString().toUpperCase() + suffix;
        //在指定位置创建一个空文件
        File destFile = new File(dir, filename);
        try {
            file.transferTo(destFile);//将file文件中的数据写入到destFile中
        } catch (IOException e) {
            try {
                throw new FileUploadException("文件读写异常");
            } catch (FileUploadException ex) {
                ex.printStackTrace();
            }
        }
        //返回头像路径/upload/test.png
        String avatar = "/upload/" + filename;
        return new JsonResult<>(200, avatar);
    }
}