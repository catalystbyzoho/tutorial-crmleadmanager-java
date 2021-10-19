function createLead() {
    debugger;
    $("#loader").show();
    var firstName = $("#firstName").val();
    var lastName = $("#lastName").val();
    if (lastName == "") {
        alert("Kindly Enter the Last Name");
        $("#loader").hide();
        return;
    }
    var companyName = $("#companyName").val();
    var email = $("#email").val();
    var state = $("#state").val();
    var phone = $("#phone").val();
    var leadSource = $("#leadSource").val();

    $.ajax({
        url: "/server/crm_crud/crmData",
        type: "post",
        contentType: "application/json",
        data: JSON.stringify({
            "First_Name": firstName,
            "Last_Name": lastName,
            "Company": companyName,
            "Email": email,
            "State": state,
            "Phone": phone,
            "Lead_Source": leadSource,
        }),
        success: function (data) {
            debugger;
            $('#leads').trigger("reset");
            $("#myModalLabel").html("<br>Success");
            $("#message").html("Lead Created Successfully");
            $("#loader").hide();
            $('#myModal').modal('show');
        },
        error: function (error) {
            $('#leads').trigger("reset");
            $("#myModalLabel").html("<br>Failure");
            $("#message").html("Please try again after Sometime");
            $("#loader").hide();
            $('#myModal').modal('show');
        }
    });
}

function logout() {
    var redirectURL = "https://leadmanager-721459367.development.catalystserverless.com/app/signin.html"; //Add your app domain
    var auth = catalyst.auth;
    auth.signOut(redirectURL);
}

function getUserDetails() {
    $("#main").hide();
    $("#connect").hide();
    catalyst.auth.isUserAuthenticated().then(result => {
        $("#loader").show();
        $.ajax({
            url: "/server/crm_crud/getUserDetails",
            type: "get",
            success: function (data) {
                $("#loader").hide();
                if (data.userId) {
                    $("#main").show();
                    $("#connect").hide();
                } else {
                    $("#connect").show();
                    $("#main").hide();
                }
            },
            error: function (error) {
                $("#myModalLabel").html("<br>Failure");
                $("#message").html("Please try again after Sometime");
                $("#loader").hide();
                $('#myModal').modal('show');
            }
        });
    }).catch(err => {
        document.body.innerHTML = 'You are not logged in. Please log in to continue. Redirecting you to the login page..';
        setTimeout(function () {
            window.location.href = "login.html";
        }, 3000);
    });
}



function getLeads() {
    debugger;
    var tableContainer = document.getElementById("showData");
    tableContainer.innerHTML = "";
    $("#loaders").show();
    $.ajax({
        url: "/server/crm_crud/crmData",
        type: "get",
        success: function (data) {
            debugger;
            var reqData = getRequiredData(data.data);
            $("#loaders").hide();
            renderTable(reqData);
        },
        error: function (error) {
            $("#myModalLabel").html("<br>Failure");
            $("#message").html("Please try again after Sometime");
            $("#loader").hide();
            $('#myModal').modal('show');
        }
    });


}

function showDeletePopup(leadID) {
    $('#ModalDanger').modal('show');
    var deleteBtn = document.getElementById("delete-btn");
    deleteBtn.value = leadID;
}

function deleteLead() {
    var leadID = document.getElementById('delete-btn').value;
    $.ajax({
        url: "/server/crm_crud/crmData/" + leadID,
        type: "delete",
        success: function (data) {
            debugger;
            $('#ModalDanger').modal('toggle');
            $("#myModalLabel").html("<br>Success");
            $("#message").html("Lead Deleted Successfully");
            $('#myModal').modal('show');
            setTimeout(function () {
                location.reload();
            }, 3000);
        },
        error: function (error) {
            $("#myModalLabel").html("<br>Failure");
            $("#message").html("Please try again after Sometime");
            $("#loader").hide();
            $('#myModal').modal('show');
        }
    });
}

function showEditPopup(leadID) {

    $.ajax({
        url: "/server/crm_crud/crmData/" + leadID,
        type: "get",
        success: function (data) {
            debugger;
            var respData = data.data;
            $('#editfirstName').val(respData[0].First_Name);
            $('#editlastName').val(respData[0].Last_Name);
            $('#editcompanyName').val(respData[0].Company);
            $('#editemail').val(respData[0].Email);
            $('#editstate').val(respData[0].State);
            $('#editphone').val(respData[0].Phone);
            $('#editleadSource').val(respData[0].Lead_Source);
            $('#editBtn').val(respData[0].id);

            $('#editfirstName').html(respData[0].First_Name);
            $('#editlastName').html(respData[0].Last_Name);
            $('#editcompanyName').html(respData[0].Company);
            $('#editemail').html(respData[0].Email);
            $('#editstate').html(respData[0].State);
            $('#editphone').html(respData[0].Phone);
            $('#editleadSource').html(respData[0].Lead_Source);

            $('#editForm').modal('show');
        },
        error: function (error) {
            $("#myModalLabel").html("<br>Failure");
            $("#message").html("Please try again after Sometime");
            $("#loader").hide();
            $('#myModal').modal('show');
        }
    });
}

function editLead() {
    var firstName = $("#editfirstName").val();
    var lastName = $("#editlastName").val();
    if (lastName == "") {
        alert("Kindly Enter the Last Name");
        return;
    }
    var companyName = $("#editcompanyName").val();
    var email = $("#editemail").val();
    var state = $("#editstate").val();
    var phone = $("#editphone").val();
    var leadSource = $("#editleadSource").val();
    var leadID = $("#editBtn").val();

    $.ajax({
        url: "/server/crm_crud/crmData/" + leadID,
        type: "put",
        contentType: "application/json",
        data: JSON.stringify({
            "First_Name": firstName,
            "Last_Name": lastName,
            "Company": companyName,
            "Email": email,
            "State": state,
            "Phone": phone,
            "Lead_Source": leadSource
        }),
        success: function (data) {
            debugger;
            $('#editForm').modal('toggle');
            $("#myModalLabel").html("<br>Success");
            $("#message").html("Lead Edited Successfully");
            $('#myModal').modal('show');
            setTimeout(function () {
                location.reload();
            }, 3000);
        },
        error: function (error) {
            $('#leads').trigger("reset");
            $("#myModalLabel").html("<br>Failure");
            $("#message").html("Please try again after Sometime");
            $("#loader").hide();
            $('#myModal').modal('show');
        }
    });

}


function getRequiredData(data) {

    var i;
    var resp = [];
    for (i = 0; i < data.length; i++) {
        var gulp = {
            "First Name": data[i].First_Name,
            "Last Name": data[i].Last_Name,
            "Phone": data[i].Phone,
            "Email": data[i].Email,
            "Company": data[i].Company,
			"Edit": '<center><a href="javascript:showEditPopup('&quot;' + data[i].id + '&quot;)">&#9998;︎</a></center>',
            "Delete": '<center><a href="javascript:showDeletePopup(&quot;' + data[i].id + '&quot;)">&#128465;︎</a></center>'
        }
        resp.push(gulp);
    }
    console.log(resp);
    return resp;
}
function renderTable(respData) {
    var col = [];
    for (var i = 0; i < respData.length; i++) {
        for (var key in respData[i]) {
            if (col.indexOf(key) === -1) {
                col.push(key);
            }
        }
    }
    var table = document.createElement("table");
    table.id = "dataTable";

    var tr = table.insertRow(-1);

    for (var i = 0; i < col.length; i++) {
        var th = document.createElement("th");
        th.innerHTML = col[i];
        tr.appendChild(th);
    }
    for (var i = 0; i < respData.length; i++) {

        tr = table.insertRow(-1);

        for (var j = 0; j < col.length; j++) {
            var tabCell = tr.insertCell(-1);
            tabCell.innerHTML = respData[i][col[j]];
        }
    }

    var divContainer = document.getElementById("showData");
    divContainer.innerHTML = "";
    divContainer.appendChild(table);
}