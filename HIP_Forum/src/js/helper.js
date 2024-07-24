export function calculateTimeDifference(dateInput) {
    const date = new Date(dateInput);
    const currentTime = new Date(); // Thời gian hiện tại
    const timeDifference = currentTime - date; // Tính khoảng cách thời gian

    // Chuyển đổi khoảng cách thời gian về đơn vị giờ
    const hoursDifference = timeDifference / (1000 * 60 * 60);
    
    if (hoursDifference >= 24) {
        // Nếu khoảng cách thời gian lớn hơn hoặc bằng 24 giờ, chuyển đổi về đơn vị ngày
        const daysDifference = Math.floor(hoursDifference / 24);
        return `${daysDifference} ngày`;
    } else {
        // Nếu khoảng cách thời gian nhỏ hơn 24 giờ, trả về đơn vị giờ
        return `${hoursDifference} giờ`;
    }
}

export function convertName(name){
    const names = name.split(" ");
    return names[0][0] + names[names.length-1][0];
}