import { Package, Save } from "lucide-react"
import "./Selling.css"

export const Selling = () => {
  return (
    <div className="selling-page">
      <div className="page-header">
        <h1>
          <Package className="page-icon" />
          Bán thuốc
        </h1>
        <p>Ghi nhận giao dịch và lưu thông tin truy xuất nguồn gốc</p>
      </div>

      {/* THÔNG TIN THUỐC */}
      <div className="section-card">
        <h2 className="section-title">Thông tin thuốc</h2>

        <div className="form-grid">
          <div className="form-group">
            <label>Mã thuốc</label>
            <input
              type="text"
              className="form-input"
              placeholder="VD: T001"
            />
          </div>

          <div className="form-group">
            <label>Tên thuốc</label>
            <input
              type="text"
              className="form-input"
              placeholder="Paracetamol"
            />
          </div>

          <div className="form-group">
            <label>Số lô (Batch)</label>
            <input
              type="text"
              className="form-input"
            />
          </div>

          <div className="form-group">
            <label>Ngày sản xuất</label>
            <input
              type="date"
              className="form-input"
            />
          </div>

          <div className="form-group">
            <label>Hạn sử dụng</label>
            <input
              type="date"
              className="form-input"
            />
          </div>

          <div className="form-group">
            <label>Nhà sản xuất</label>
            <input
              type="text"
              className="form-input"
            />
          </div>

          <div className="form-group">
            <label>Số lượng bán</label>
            <input
              type="number"
              className="form-input"
            />
          </div>
        </div>
      </div>

      {/* THÔNG TIN GIAO DỊCH */}
      <div className="section-card">
        <h2 className="section-title">Thông tin giao dịch</h2>

        <div className="form-grid">
          <div className="form-group">
            <label>Mã giao dịch</label>
            <input
              type="text"
              className="form-input"
            />
          </div>

          <div className="form-group">
            <label>Ngày giờ bán</label>
            <input
              type="datetime-local"
              className="form-input"
            />
          </div>

          <div className="form-group">
            <label>Giá bán</label>
            <input
              type="number"
              className="form-input"
            />
          </div>

          <div className="form-group">
            <label>Tổng tiền</label>
            <input
              type="number"
              className="form-input"
            />
          </div>

          <div className="form-group">
            <label>Phương thức thanh toán</label>
            <select className="form-select">
              <option>Tiền mặt</option>
              <option>Chuyển khoản</option>
              <option>Ví điện tử</option>
            </select>
          </div>

          <div className="form-group">
            <label>Hash Blockchain</label>
            <input
              type="text"
              className="form-input"
            />
          </div>
        </div>
      </div>

      {/* THÔNG TIN HIỆU THUỐC */}
      {/* <div className="section-card">
        <h2 className="section-title">Thông tin hiệu thuốc</h2>

        <div className="form-grid">

          <div className="form-group">
            <label>Mã hiệu thuốc</label>
            <input type="text" className="form-input"/>
          </div>

          <div className="form-group">
            <label>Tên hiệu thuốc</label>
            <input type="text" className="form-input"/>
          </div>

          <div className="form-group">
            <label>Số điện thoại</label>
            <input type="text" className="form-input"/>
          </div>

          <div className="form-group">
            <label>Địa chỉ</label>
            <input type="text" className="form-input"/>
          </div>

          <div className="form-group">
            <label>Nhân viên bán</label>
            <input type="text" className="form-input"/>
          </div>

        </div>
      </div> */}

      {/* THÔNG TIN NGƯỜI MUA */}
      <div className="section-card">
        <h2 className="section-title">Thông tin người mua</h2>

        <div className="form-grid">
          <div className="form-group">
            <label>Tên khách hàng</label>
            <input
              type="text"
              className="form-input"
            />
          </div>

          <div className="form-group">
            <label>Số điện thoại</label>
            <input
              type="text"
              className="form-input"
            />
          </div>

          <div className="form-group">
            <label>Địa chỉ</label>
            <input
              type="text"
              className="form-input"
            />
          </div>

          <div className="form-group">
            <label>Đơn thuốc</label>
            <input
              type="text"
              className="form-input"
            />
          </div>

          <div className="form-group">
            <label>Bác sĩ kê đơn</label>
            <input
              type="text"
              className="form-input"
            />
          </div>
        </div>
      </div>

      {/* BUTTON */}
      <div className="form-actions">
        <button className="btn btn-primary create-btn">
          <Save size={16} />
          Lưu giao dịch & Ghi blockchain
        </button>
      </div>
    </div>
  )
}
