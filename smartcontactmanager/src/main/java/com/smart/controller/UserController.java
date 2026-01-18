package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.MyOrderRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.MyOrder;
import com.smart.entities.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpSession;
import com.razorpay.*;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ContactRepository contactRepository;
	
	@Autowired
	private MyOrderRepository myOrderRepository;

	// method for adding common data to response
	@ModelAttribute
	public void addCommonData(Model m, Principal principal) {
		String userName = principal.getName();
		System.out.println(userName);
		// get the user using username(Email)
		User user = userRepository.getUserByUserName(userName);
		System.out.println(user);

		m.addAttribute("user", user);
	}

	// dashboard home
	@RequestMapping("/index")
	public String dashboard(Model m, Principal principal) {
		m.addAttribute("title", "User Dashboard");
		return "normal/user_dashboard";
	}

	// open add form handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model m) {
		m.addAttribute("title", "Add Contact");
		m.addAttribute("contact", new Contact());
		return "normal/add_contact_form";
	}

	// processing add contact form
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
			Principal principal, HttpSession session) {

		try {
			String name = principal.getName();
			User user = this.userRepository.getUserByUserName(name);

			// processing and uploading file
			if (file.isEmpty()) {
				// if the file is empty then try our message
				//System.out.println("file is empty");       //printing message on console
				contact.setImage("contact.jpg");
			} else {
				
				// Checking file size (1,048,576 bytes = 1MB)
				if (file.getSize() > 1048576) {
				    session.setAttribute("message", new Message("File size is too large! Please upload less than 1MB.", "warning"));
				    return "normal/add_contact_form";
				}
				
				
				// upload the file to folder and update the name to contact
				String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));  //getting current timedate

				contact.setImage(timestamp + "_" + file.getOriginalFilename());                                 //add current timeadate with file name
				
				
				File savefile = new ClassPathResource("static/image").getFile();                                //getting the folder path
				Path path = Paths.get(
						savefile.getAbsolutePath() + File.separator + timestamp + "_" + file.getOriginalFilename());    // getting proper path with file name
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);                           // copy the file in our given folder (file in bytes,our proper path with filename,set an option to- if if file is exist with same name then replace that file to new file)

				
				//System.out.println("image is uploaded");     //printing on console
			}

			contact.setUser(user); // add user in contact table
			user.getContacts().add(contact); // add contact in user table

			this.userRepository.save(user);       //here when we save the user automatically contact will be save because of the link

			//System.out.println("Added to database");		//printing on console

			// message success.....
			session.setAttribute("message", new Message("Your cantact is added !! Add more..", "success"));

		} catch (Exception e) {
			//System.out.println("ERROR " + e.getMessage());    //printing on console
			e.printStackTrace();
			// error message....
			session.setAttribute("message", new Message("Some thing went wrong !! try again..", "danger"));
		}
		return "normal/add_contact_form";
	}

	
	// show contacts handler
	// per page =[n]
	// current page = 0 [page]
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page, Model m, Principal principal) {
		m.addAttribute("title", "Show User Contacts");
		// contact ki list ko bhejni hai

		String username = principal.getName();
		User user = this.userRepository.getUserByUserName(username);

		Pageable pageable = PageRequest.of(page, 5); // number of row showing

		Page<Contact> contacts = this.contactRepository.findContactsByUser(user.getId(), pageable);

		m.addAttribute("contacts", contacts);
		m.addAttribute("currentpage", page);
		m.addAttribute("totalpages", contacts.getTotalPages());

		return "normal/show_contacts";
	}

	
	// showing perticular contact details.
	@RequestMapping("/{cId}/contact")
	public String showContactDetails(@PathVariable("cId") Integer cId, Model m, Principal principal) {
		
		//System.out.println(cId);						//printing on console

		//getting contact object
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();

		//getting user object
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);

		if (user.getId() == contact.getUser().getId()) {
			m.addAttribute("contact", contact);
			m.addAttribute("title", contact.getName());
		}

		return "normal/contact_detail";
	}

	// delete contact handler
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid") Integer cid, HttpSession session, Principal principal) {

		try {
			Optional<Contact> contactOptional = this.contactRepository.findById(cid);

			if (contactOptional.isPresent()) {
				Contact contact = contactOptional.get();

				// Check security: user should only delete their own contacts
				String userName = principal.getName();
				User user = this.userRepository.getUserByUserName(userName);

				if (user.getId() == contact.getUser().getId()) {

					// --- NEW CODE: DELETE IMAGE FROM FOLDER ---
					String imagename = contact.getImage();

					// Only attempt to delete if the image is not the default placeholder
					if (imagename != null && !imagename.equals("contact.jpg") && !imagename.isEmpty()) {
						try {
							// Get the static/image folder path
							File saveFile = new ClassPathResource("static/image").getFile();

							// Create the path to the specific file
							File fileToDelete = new File(saveFile, imagename);

							// Check if file exists and delete it
							if (fileToDelete.exists()) {
								fileToDelete.delete();
								//System.out.println("Image deleted: " + imagename);        //printing on console
							}
						} catch (Exception e) {
							System.out.println("Error deleting image file: " + e.getMessage());
							// We don't stop the process; we still want to delete the DB record
						}
					}
					// ------------------------------------------

					// Unlink and Delete from DB
					contact.setUser(null); 								// unlink contact from user
					this.contactRepository.delete(contact);

					session.setAttribute("message",
							new Message("Contact and associated image deleted Successfully...", "success"));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			session.setAttribute("message", new Message("Something went wrong on the server!", "danger"));
		}

		return "redirect:/user/show-contacts/0";
	}

	// open update form handler
	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cid, Model m) {

		m.addAttribute("title", "Update Contact");
		Contact contact = this.contactRepository.findById(cid).get();
		m.addAttribute("contact", contact);

		return "normal/update_form";
	}
	
	
	

	// update contact handler
	@RequestMapping(value = "/process-update", method = RequestMethod.POST)
	public String updateHandler(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
			Model m, HttpSession session, Principal principal) {

		try {
			// old contact detail
			Contact oldcontactDetail = this.contactRepository.findById(contact.getcId()).get();

			// image..
			if (!file.isEmpty()) {
				// file work..
				// rewrite
				
				// Checking file size (1,048,576 bytes = 1MB)
				if (file.getSize() > 1048576) {
				    session.setAttribute("message", new Message("File size is too large! Please upload less than 1MB.", "warning"));
				    return "redirect:/user/" + contact.getcId() + "/contact";
				}

				// delete old photo

				File deletefile = new ClassPathResource("static/image").getFile();
				File file1 = new File(deletefile, oldcontactDetail.getImage());
				file1.delete();

				// update new photo
				String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

				File savefile = new ClassPathResource("static/image").getFile();

				Path path = Paths.get(
						savefile.getAbsolutePath() + File.separator + timestamp + "_" + file.getOriginalFilename());

				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

				contact.setImage(timestamp + "_" + file.getOriginalFilename());

			} else {
				contact.setImage(oldcontactDetail.getImage());
			}
			
			User user = this.userRepository.getUserByUserName(principal.getName());     //getting user refference
			contact.setUser(user);														//in new(updated) contact set the user-reference(user_id)

			this.contactRepository.save(contact);

			session.setAttribute("message", new Message("Your contact is updated..", "success"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "redirect:/user/" + contact.getcId() + "/contact";
	}
	
	
	//your profile handler
	@GetMapping("/profile")
	public String yourProfile(Model m) {
		m.addAttribute("title","Profile Page");
		return "normal/profile";
	}
	
	
	
	//open setting handler
	@GetMapping("/settings")
	public String openSettings() {
		return "normal/settings";
	}
	
	
	
	//change password... handler
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldpassword") String oldpassword,@RequestParam("newpassword") String newpassword,Principal principal,HttpSession session) {
		
		System.out.println("old password "+oldpassword);
		System.out.println("new password "+newpassword);
		
		String username = principal.getName();
		User currentUser = this.userRepository.getUserByUserName(username);
		System.out.println(currentUser.getPassword());
		
		if(this.bCryptPasswordEncoder.matches(oldpassword, currentUser.getPassword())) {
			//change password
			currentUser.setPassword(this.bCryptPasswordEncoder.encode(newpassword));
			this.userRepository.save(currentUser);
			session.setAttribute("message", new Message("your password is successfully changed..","success"));
			
		}else {
			//error..
			session.setAttribute("message", new Message("Please enter correct old password....","danger"));
			return "redirect:/user/settings";
		}
		
		
		return "redirect:/user/index";
	}
	
	
	//creating order for payment
	@PostMapping("/create_order")
	@ResponseBody
	public String createOreder(@RequestBody Map<String, Object> data,Principal principal) throws RazorpayException {
		//System.out.println("hey order function executed");
		
		//System.out.println(data);
		
		int amt = Integer.parseInt(data.get("amount").toString());
		
		var client=new RazorpayClient("rzp_test_S479CA3Pt5kGUq", "dhuDfJIwedrry5uQgC9E2y29");
		
		JSONObject ob=new JSONObject();
		ob.put("amount", amt*100);
		ob.put("currency", "INR");
		ob.put("receipt", "txn_235425");
		
		//creating new order
		Order order = client.orders.create(ob);
		//System.out.println(order);
		
		//save the order in the database
		
		MyOrder myOrder = new MyOrder();
		
		myOrder.setAmount(order.get("amount")+"");
		myOrder.setOrderId(order.get("id"));
		myOrder.setPaymentId(null);
		myOrder.setStatus("created");
		myOrder.setUser(this.userRepository.getUserByUserName(principal.getName()));
		myOrder.setReceipt(order.get("receipt"));
		
		this.myOrderRepository.save(myOrder);		
		
		//if you want you can save this to your database
		
		return order.toString();
	}
	
	//
	@PostMapping("/update_order")
	public ResponseEntity<?> upadateOrder(@RequestBody Map<String,Object> data){
		
		MyOrder myorder = this.myOrderRepository.findByOrderId(data.get("order_id").toString());
		
		myorder.setPaymentId(data.get("payment_id").toString());
		myorder.setStatus(data.get("status").toString());
		
		this.myOrderRepository.save(myorder);
		
		System.out.println(data);
		
		return ResponseEntity.ok(Map.of("msg","updated"));
	} 
}
