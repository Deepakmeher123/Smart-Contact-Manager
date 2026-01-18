console.log("this is script file")

const toggleSidebar = () => {
    if ($('.sidebar').is(":visible")) {
        //true
        $(".sidebar").css("display", "none");
        $(".content").css("margin-left", "0%");
    } else {
        //false
        $(".sidebar").css("display", "block");
        $(".content").css("margin-left", "20%");
    }
};


const search = () => {
    //console.log("searching")

    let query = $("#search-input").val();
    //console.log(query);

    if (query == "") {
        $(".search-result").hide();
    } else {
        console.log(query);
        //sending request to server
        let url = `http://localhost:8080/search/${query}`;
        fetch(url).then((response) => {
            return response.json();
        })
            .then((data) => {
                //data......
                //console.log(data);

                let text = `<div class='list-group'>`

                data.forEach((contact) => {
                    text += `<a href='/user/${contact.cId}/contact' class='list-group-item list-group-item-action'>${contact.name}</a>`;
                });

                text += `</div>`;

                $(".search-result").html(text);
                $(".search-result").show();

            });

        $(".search-result").show();
    }
}



//payment system
//first request to server to create order
const paymentStart = () => {
    console.log("payment started...");
    let amount = $("#payment_field").val();
    console.log(amount);
    if (amount == "" || amount == null) {
        //alert("amount is required !!");
        Swal.fire({
            title: "Failed!!",
            text: "Oops Amount Required.",
            icon: "error"
        });
        return;
    }

    //code..
    //we will use ajax to send request to server to create order - jquery

    $.ajax(
        {
            url: '/user/create_order',
            data: JSON.stringify({ amount: amount, info: 'order_request' }),
            contentType: 'application/json',
            type: 'POST',
            dataType: 'json',
            success: function(response) {
                //invoked where success
                console.log(response)
                if (response.status == "created") {
                    //open payment form
                    let options = {
                        key: 'rzp_test_S479CA3Pt5kGUq',
                        amount: response.amount,
                        currency: 'INR',
                        name: "Smart Contact Manager",
                        description: 'Donation',
                        image: "https://www.paysafe.com/fileadmin/content/images/2024/blog/Paysafe_Insights_Imagery/Popular_payment_methods.jpg",
                        order_id: response.id,
                        handler: function(response) {
                            console.log(response.razorpay_payment_id)
                            console.log(response.razorpay_order_id)
                            console.log(response.razorpay_signature)
                            console.log("payment successfull !!")
                            //alert("congrats payment successfull !!")

                             updatePaymentOnServer(response.razorpay_payment_id,response.razorpay_order_id,'paid');

                            Swal.fire({
                                title: "congrats payment successfull !!",
                                icon: "success",
                                draggable: true
                            });
                        },
                        prefill: {
                            name: "",
                            email: "",
                            contact: ""
                        },
                        notes: {
                            address: "code with Deepak..!"
                        },
                        theme: {
                            color: "#3399cc"
                        }
                    };

                    let rzp = new Razorpay(options);

                    rzp.on('payment.failed', function(response) {
                        alert(response.error.code);
                        alert(response.error.description);
                        alert(response.error.source);
                        alert(response.error.step);
                        alert(response.error.reason);
                        alert(response.error.metadata.order_id);
                        alert(response.error.metadata.payment_id);
                        //alert("Oops Payment failed!!");
                        Swal.fire({
                            title: "Failed!!",
                            text: "Oops Payment failed.",
                            icon: "error"
                        });
                    });

                    rzp.open();
                }
            },
            error: function(error) {
                //invoked when error
                console.log(error)
                alert("something went wrong !!")
            }
        }
    )

};

function updatePaymentOnServer(payment_id,order_id,status)
{
    $.ajax({
         url: '/user/update_order',
            data: JSON.stringify({ payment_id:payment_id, order_id:order_id,status:status, }),
            contentType: 'application/json',
            type: 'POST',
            dataType: 'json',
            success:function(response){
                 Swal.fire({
                                title: "congrats payment successfull !!",
                                icon: "success",
                                draggable: true
                            });
            },
            error:function(error){
                Swal.fire({
                            title: "Failed!!",
                            text: "Your payment is successful , but we did not get on server , we will contact you as soon as possible.",
                            icon: "error"
                        });
            },
    });
}