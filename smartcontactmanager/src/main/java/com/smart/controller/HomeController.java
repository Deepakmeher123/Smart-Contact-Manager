package com.smart.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.dao.UserRepository;
import com.smart.entities.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class HomeController {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@RequestMapping("/")
	public String home(Model m) {
		m.addAttribute("title", "home- smart contact manager");
		return "home";
	}

	@RequestMapping("/about")
	public String about(Model m) {
		m.addAttribute("title", "about- smart contact manager");
		return "about";
	}

	@RequestMapping("/signup")
	public String signup(Model m) {
		m.addAttribute("title", "Register- smart contact manager");
		m.addAttribute("user", new User());
		return "signup";
	}

	// this handler for registring user
	@RequestMapping(value = "/do_register", method = RequestMethod.POST)
	public String registeruser(@Valid @ModelAttribute("user") User user, BindingResult result1,
			@RequestParam(value = "agreement", defaultValue = "false") boolean agreement, Model m,
			HttpSession session) {
		try {

			if (result1.hasErrors()) {
				// System.out.println("ERROR " + result1.toString()); //for showing in console
				m.addAttribute("user", user);
				return "signup";
			}

			if (!agreement) {
				// System.out.println("you have not agreed the terms and conditions."); //for
				// showing in console
				throw new Exception("You have not agreed the terms and conditions");
			}

			user.setRole("ROLE_USER");
			user.setEnabled(true);
			user.setImageUrl("default.png");
			user.setPassword(passwordEncoder.encode(user.getPassword()));

//			System.out.println("Agreement " + agreement);			//for showing in console
//			System.out.println(user);								//for showing in console

			this.userRepository.save(user);

			m.addAttribute("user", new User()); // here sending new user object to showing empty fields and showing
												// successful message(degfine in next line)

			session.setAttribute("message", new Message("Successfully Registered !!", "alert-success")); // showing
																											// successful
																											// message
			return "signup";
		} catch (Exception e) {
			e.printStackTrace();
			m.addAttribute("user", user);
			session.setAttribute("message", new Message("Somthing went worong !!" + e.getMessage(), "alert-danger"));
			return "signup";
		}

	}

	// handler for custom login
	@GetMapping("/signin")
	public String customLogin(Model m) {
		m.addAttribute("title", "Login Page");
		return "login";
	}

}
